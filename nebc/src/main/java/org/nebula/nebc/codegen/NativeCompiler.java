package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTargetDataRef;
import org.bytedeco.llvm.LLVM.LLVMTargetMachineRef;
import org.bytedeco.llvm.LLVM.LLVMTargetRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.llvm.global.LLVM.*;

/**
 * Orchestrates the "LLVM module → native binary" pipeline.
 * <ol>
 * <li>Initialises the LLVM native target back-end.</li>
 * <li>Emits an ELF object file ({@code .o}) via
 * {@code LLVMTargetMachineEmitToFile}.</li>
 * <li>Invokes {@code clang} to link the object into a final executable.</li>
 * </ol>
 *
 * <p>
 * This class is stateless — each call to {@link #compile} is self-contained.
 */
public final class NativeCompiler {

	private NativeCompiler() {
		// Utility class
	}

	/**
	 * Compiles the given LLVM module into a native executable.
	 *
	 * @param module     The verified LLVM module to compile.
	 * @param outputPath The desired path for the output binary (e.g. "a.out").
	 * @param triple     The LLVM target triple, or {@code null} for the host
	 *                   default.
	 * @param isStatic   Whether to link for a static, freestanding binary
	 *                   (nostdlib).
	 * @throws CodegenException if target initialisation, object emission, or
	 *                          linking fails.
	 */
	public static void compile(LLVMModuleRef module, String outputPath, String triple, boolean isStatic,
			boolean isLibrary,
			List<Path> additionalObjects) {
		// 1. Initialise LLVM targets
		LLVMInitializeNativeTarget();
		LLVMInitializeNativeAsmPrinter();
		LLVMInitializeNativeAsmParser();

		// 2. Resolve target triple
		BytePointer defaultTriple = LLVMGetDefaultTargetTriple();
		String targetTriple = (triple != null && !triple.isBlank()) ? triple : defaultTriple.getString();
		LLVMSetTarget(module, targetTriple);

		// 3. Look up the target
		LLVMTargetRef target = new LLVMTargetRef();
		BytePointer errorMsg = new BytePointer();
		if (LLVMGetTargetFromTriple(new BytePointer(targetTriple), target, errorMsg) != 0) {
			String msg = errorMsg.getString();
			LLVMDisposeMessage(errorMsg);
			LLVMDisposeMessage(defaultTriple);
			throw new CodegenException("Failed to get LLVM target for triple '" + targetTriple + "': " + msg);
		}

		// 4. Create the target machine
		LLVMTargetMachineRef machine = LLVMCreateTargetMachine(
				target,
				targetTriple,
				"generic", // CPU
				"", // features
				LLVMCodeGenLevelAggressive,
				LLVMRelocPIC, // Position-independent code
				LLVMCodeModelDefault);

		if (machine == null || machine.isNull()) {
			LLVMDisposeMessage(defaultTriple);
			throw new CodegenException("Failed to create LLVM target machine.");
		}

		// 5. Set the module data layout from the machine
		LLVMTargetDataRef dataLayout = LLVMCreateTargetDataLayout(machine);
		LLVMSetModuleDataLayout(module, dataLayout);

		// 6. Emit object file to a temp path
		Path objectFile;
		try {
			objectFile = Files.createTempFile("nebula_", ".o");
		} catch (IOException e) {
			LLVMDisposeTargetMachine(machine);
			LLVMDisposeMessage(defaultTriple);
			throw new CodegenException("Failed to create temporary object file.", e);
		}

		BytePointer emitError = new BytePointer();
		if (LLVMTargetMachineEmitToFile(machine, module, new BytePointer(objectFile.toString()),
				LLVMObjectFile, emitError) != 0) {
			String msg = emitError.getString();
			LLVMDisposeMessage(emitError);
			LLVMDisposeTargetMachine(machine);
			LLVMDisposeMessage(defaultTriple);
			throw new CodegenException("Failed to emit object file: " + msg);
		}
		LLVMDisposeMessage(emitError);

		// 7. Clean up LLVM machine resources
		LLVMDisposeTargetMachine(machine);
		LLVMDisposeMessage(defaultTriple);

		// 8. Link with clang
		link(objectFile, outputPath, isStatic, isLibrary, additionalObjects);
	}

	/**
	 * Invokes {@code clang} to link the object file into a native executable.
	 */
	private static void link(Path objectFile, String outputPath, boolean isStatic, boolean isLibrary,
			List<Path> additionalObjects) {
		try {
			List<String> command = new ArrayList<>(List.of(
					"clang",
					"-O3", // add optimization
					objectFile.toString(),
					"-o", outputPath));

			if (additionalObjects != null) {
				for (Path p : additionalObjects) {
					command.add(p.toString());
				}
			}

			// Always enforce freestanding environment
			command.add("-nostdlib");
			command.add("-nostartfiles");

			if (isLibrary) {
				command.add("-shared");
				command.add("-fPIC");
			} else {
				if (isStatic) {
					command.add("-static");
				} else {
					command.add("-no-pie");
				}
				command.add("-Wl,-rpath,.");
				command.add("-L.");
				command.add("-lneb");
			}

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.inheritIO();
			Process process = pb.start();
			int exitCode = process.waitFor();

			if (exitCode != 0) {
				throw new CodegenException(
						"Linker (clang) failed with exit code " + exitCode +
								". Ensure clang is installed and available on PATH.");
			}
		} catch (IOException e) {
			throw new CodegenException(
					"Failed to invoke linker (clang). Ensure clang is installed and available on PATH.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CodegenException("Linking interrupted.", e);
		} finally {
			// Clean up temp object file
			try {
				Files.deleteIfExists(objectFile);
			} catch (IOException ignored) {
				// Best effort cleanup
			}
		}
	}
}
