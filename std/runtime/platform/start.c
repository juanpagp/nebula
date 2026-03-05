extern int main(int argc, void** argv);
extern void sys_exit(int code);

/*
 * _start is the ELF entry point — jumped to by the kernel, NOT called.
 * At entry, RSP points to argc (System V x86-64 ABI initial process stack).
 * We pop argc into RDI and move the updated RSP (pointing at argv[0]) into
 * RSI before calling main(argc, argv), satisfying the SysV calling convention.
 * Passing argc/argv unconditionally is harmless — main bodies that declare no
 * parameters simply ignore the incoming registers.
 * Using naked avoids any compiler-generated prolog that would corrupt RSP.
 *
 * Stack alignment: the x86-64 SysV ABI requires RSP to be 16-byte aligned
 * at every CALL instruction. After popping argc (RSP += 8), the alignment
 * might be off by 8 bytes relative to what shared libraries using SSE/AVX
 * instructions (e.g. GLFW, OpenGL) expect. We therefore align RSP downward
 * to the nearest 16-byte boundary before transferring control to main, just
 * as glibc's crt1.o does with its "and $-16, %rsp" before calling
 * __libc_start_main. The previously-saved argv pointer (rsi) remains valid
 * because it points into the original, immutable process stack frame.
 */
__attribute__((naked)) void _start()
{
    __asm__ volatile (
        "xor %rbp, %rbp\n\t"
        "pop %rdi\n\t"
        "mov %rsp, %rsi\n\t"
        "and $-16, %rsp\n\t"
        "call main\n\t"
        "mov %eax, %edi\n\t"
        "call sys_exit\n\t"
        "ud2\n\t"
    );
}
