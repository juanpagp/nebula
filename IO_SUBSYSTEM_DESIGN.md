# Nebula I/O Subsystem Design Document

## Overview

The Nebula I/O subsystem is a **memory-safe, CVT-aware** input/output framework designed for the Nebula systems programming language. It provides type-safe printing primitives while respecting Nebula's Causal Validity Tracking (CVT) memory model.

**Key Design Principles:**
- **Zero libc dependencies**: Uses only OS syscalls (Windows WriteFile, Unix write)
- **CVT safety**: All heap allocations are tracked and freed by Last-Usage Analysis
- **Generic and extensible**: Any type implementing `Stringable` can be printed
- **Minimal and focused**: print/println only; format strings planned for future

---

## Architecture

### Layer 1: Runtime Shim (C)
**File:** `runtime.c`

The lowest layer provides platform-specific syscall wrappers.

```c
void __nebula_rt_write(const unsigned char *buf, int len)
```

**Platforms:**
- **Windows**: Uses `WriteFile(GetStdHandle(STD_OUTPUT_HANDLE), ...)`
- **Unix/Linux**: Uses `write(1, buf, len)` syscall directly
- **No libc**: No stdio.h, printf, or C stdlib dependencies

**Why direct syscalls?**
- Avoid libc overhead
- Ensure predictable behavior
- Enable FFI with guaranteed ABI
- Support embedded/freestanding environments

### Layer 2: FFI Bridge (Nebula)
**File:** `std/sys.neb`

Declares the C runtime functions for use in Nebula code.

```nebula
private extern "C" {
    void __nebula_rt_write(keeps Ref<u8> buf, i32 len);
}
```

**Key insight:** The `keeps` keyword tells the CVT checker that:
- The function reads the buffer but does not own it
- The caller retains all responsibility for freeing the buffer
- The function cannot store pointers to the buffer beyond its return

### Layer 3: Type Conversion (Nebula)
**File:** `std/primitives.neb`

Implements the `Stringable` trait for built-in types.

**Traits:**
- `Stringable for i32` → Uses `__nebula_itoa_i32()` (manual integer-to-string)
- `Stringable for u32` → Uses `__nebula_itoa_u32()`
- `Stringable for u8` → Uses `__nebula_itoa_u8()`
- `Stringable for string` → Identity (already a string)

**Integer-to-String Algorithm:**
```
__nebula_itoa_i32(42):
1. Allocate Region<u8> buffer
2. Handle sign: 42 is positive, no '-' prefix
3. Extract digits: 42 % 10 = 2, 42 / 10 = 4; 4 % 10 = 4, 4 / 10 = 0
4. Buffer = ['2', '4'] (reverse order)
5. Reverse in-place: ['4', '2']
6. Null-terminate: ['4', '2', '\0']
7. Return as Region<u8>
```

**No sprintf:** We implement itoa manually to avoid any libc dependency. This is crucial for embedded/freestanding scenarios.

### Layer 4: Trait Definition (Nebula)
**File:** `std/traits.neb`

Defines the `Stringable` interface that all printable types must implement.

```nebula
trait Stringable {
    string toString();  // Convert self to string representation
}
```

### Layer 5: Public API (Nebula)
**File:** `std/io.neb`

Provides generic `print<T>` and `println<T>` functions.

```nebula
public void print<T: Stringable>(T item) {
    string s = item.toString();
    Ref<u8> buf = s.getRawRef();
    i32 len = s.length();
    std::sys::__nebula_rt_write(keeps buf, len);
    // Last-Usage: s is freed here
}

public void println<T: Stringable>(T item) {
    print(item);
    print("\n");
}
```

---

## CVT Safety Analysis

### Challenge: Temporary String Lifetimes

When we print an integer, toString() allocates a temporary Region on the heap:

```nebula
print(42);
  ↓
i32::toString()
  ↓
Allocates Region<u8> in heap (Valid state)
  ↓
Returns string
  ↓
Passes to __nebula_rt_write()
  ↓
Function returns
  ↓
??? Region is freed? When? By whom? Dangling pointer?
```

### Solution: Last-Usage Analysis

**The Nebula compiler performs Last-Usage Analysis to determine when a Region can be freed.**

In the print function:

```nebula
void print<T: Stringable>(T item) {
    string s = item.toString();           // Line 2
    Ref<u8> buf = s.getRawRef();          // Line 3
    i32 len = s.length();                 // Line 4
    std::sys::__nebula_rt_write(keeps buf, len);  // Line 5
    // <-- Line 6: Last-Usage of 's' is on line 4 (s.length())
    //     Compiler inserts: s.free();
}
```

**Key insight:** The `getRawRef()` call extracts a pointer, and `length()` is the last actual use of the string object. After that, the Region is deallocated.

But wait—buf still points to freed memory! How is that safe?

**Answer:** We don't use `buf` after the syscall. The `keeps` keyword tells the compiler that `__nebula_rt_write()` only reads the buffer during the call itself and does not store pointers for later. So:

1. Line 5: __nebula_rt_write() is called with buf
2. Inside the syscall: buf is read, bytes are written to stdout
3. Return from syscall: buf is no longer needed
4. Line 6: s.free() is called, invalidating the Region
5. buf becomes a dangling pointer, but we never use it

**This is sound because:**
- The CVT checker verifies that buf is not used after its Region is freed
- The syscall completes before the free
- No Use-After-Free bugs are possible

---

## Example Usage

### Printing Integers

```nebula
use std::io;

i32 x = 42;
io::print(x);           // Output: 42
io::println(x);         // Output: 42\n

io::print(-17);         // Output: -17
io::println(0);         // Output: 0\n
```

### Printing Strings

```nebula
string msg = "Hello, World!";
io::print(msg);         // Output: Hello, World!
io::println(msg);       // Output: Hello, World!\n
```

### Mixed Printing

```nebula
use std::io;

io::print("Value: ");
io::println(42);        // Output: Value: 42\n
```

---

## File Inventory

| File | Purpose | Lines | Key Functions |
|------|---------|-------|----------------|
| `runtime.c` | OS syscall wrappers | ~70 | `__nebula_rt_write()` |
| `std/sys.neb` | FFI bindings | ~50 | `extern "C" __nebula_rt_write()` |
| `std/traits.neb` | Stringable trait | ~35 | `trait Stringable` |
| `std/primitives.neb` | Type conversions | ~200+ | `__nebula_itoa_*()`, `impl Stringable` |
| `std/io.neb` | Public API | ~100+ | `print<T>()`, `println<T>()` |

**Total: ~450 lines of production code**

---

## Design Decisions & Rationale

### 1. Why Manual itoa Instead of sprintf?

**Decision:** Implement integer-to-string conversion manually.

**Rationale:**
- No libc dependency
- Predictable behavior
- Avoids format string vulnerabilities
- Enables freestanding/embedded compilation

**Trade-off:** More code, but safer and more portable.

### 2. Why Keep Namespace Separation?

**Decision:** Split into std::sys, std::traits, std::primitives, std::io

**Rationale:**
- Clear separation of concerns
- Easy to extend (e.g., read functions to sys)
- Users can import only what they need
- Trait system enables user-defined Stringable types

### 3. Why Ref<u8> Instead of string for Buffer?

**Decision:** FFI accepts `Ref<u8>` (raw pointer), not string

**Rationale:**
- Raw pointers are guaranteed stable across language boundaries
- string type may change (region header, metadata, etc.)
- Matches C ABI exactly
- Clearer that we're doing pointer manipulation

### 4. Why 'keeps' Instead of 'drops' for buf?

**Decision:** `__nebula_rt_write(keeps Ref<u8> buf, ...)`

**Rationale:**
- The function reads the buffer but does NOT own or free it
- The caller is responsible for freeing the Region
- 'keeps' signals to CVT that this is a borrow
- Prevents accidental double-free or use-after-free in FFI code

---

## Future Extensions

### Phase 2: Input Functions

```nebula
// Read a line from stdin
string readLine() {
    // Allocate Region
    // Call __nebula_rt_read()
    // Return as string
}
```

### Phase 3: File I/O

```nebula
i32 open(keeps string path, i32 flags);
void close(i32 fd);
i32 read(i32 fd, drops Ref<u8> buf, i32 maxLen);
i32 write(i32 fd, keeps Ref<u8> buf, i32 len);
```

### Phase 4: Format Strings

```nebula
void printf(keeps string format, ...args);
// Requires variadic generics support in Nebula
```

Example:
```nebula
printf("Value: %d, String: %s\n", 42, "hello");
```

### Phase 5: Custom Stringable Implementations

Users can implement Stringable for their own types:

```nebula
struct Point {
    i32 x;
    i32 y;
}

impl Stringable for Point {
    string toString() {
        string xs = x.toString();
        string ys = y.toString();
        return "Point(" + xs + ", " + ys + ")";
    }
}

Point p = Point{x: 10, y: 20};
io::println(p);  // Output: Point(10, 20)\n
```

---

## Testing Strategy

### Unit Tests

**Test: Integer Conversion**
```nebula
#[test]
fn test_itoa_i32_positive() {
    string s = 42.toString();
    assert_eq!(s, "42");
}

#[test]
fn test_itoa_i32_negative() {
    string s = (-17).toString();
    assert_eq!(s, "-17");
}

#[test]
fn test_itoa_i32_zero() {
    string s = 0.toString();
    assert_eq!(s, "0");
}

#[test]
fn test_itoa_i32_min() {
    string s = (-2147483648).toString();
    assert_eq!(s, "-2147483648");
}

#[test]
fn test_itoa_i32_max() {
    string s = 2147483647.toString();
    assert_eq!(s, "2147483647");
}
```

**Test: Print Output**
```nebula
#[test]
fn test_print_integer() {
    // Capture stdout (would require test harness)
    io::print(42);
    // Verify output == "42"
}

#[test]
fn test_println_integer() {
    // Capture stdout
    io::println(42);
    // Verify output == "42\n"
}
```

### Integration Tests

Compile a Nebula program that uses the I/O subsystem:

```nebula
// examples/hello.neb
use std::io;

i32 main() {
    io::println("Hello, World!");
    io::print("Numbers: ");
    for (i32 i = 0; i < 5; i = i + 1) {
        io::print(i);
        if (i < 4) io::print(", ");
    }
    io::println("");
    return 0;
}
```

Expected output:
```
Hello, World!
Numbers: 0, 1, 2, 3, 4
```

---

## Compatibility Notes

### Platforms

- **Linux/Unix**: Uses POSIX write(2) syscall
- **Windows**: Uses Windows API WriteFile + GetStdHandle
- **macOS**: Uses POSIX write(2) syscall (BSD-compatible)
- **WebAssembly**: Would require custom __nebula_rt_write (not yet implemented)

### Character Encoding

- **Assumption:** All strings are UTF-8
- **Current:** Integer printing assumes ASCII-compatible output (0-9)
- **Future:** Full UTF-8 support for custom types

### Buffer Sizes

- **Integer strings:** Max 12 bytes (i32: "-2147483648" + null)
- **Syscall behavior:** May write fewer bytes than requested on some systems
  - Caller responsible for retry loops (handled in C shim)

---

## CVT Annotations Summary

| Function | Parameter | Annotation | Meaning |
|----------|-----------|------------|---------|
| `__nebula_rt_write` | buf | `keeps Ref<u8>` | Read-only borrow; not freed |
| `print<T>` | item | implicit `keeps T` | Generic type is borrowed |
| `toString` (all) | self | implicit | Method receiver, borrowing receiver |

---

## Performance Considerations

### Allocations Per Print

```nebula
io::print(42);
```

**Heap allocations:**
1. Region<u8> allocated in __nebula_itoa_i32() (~12 bytes)
1. Region freed by Last-Usage Analysis before function returns

**Syscalls:**
1. write(2) syscall to kernel

**Total time:** ~1µs (microsecond) for integer-to-string, ~1-10µs for syscall

### No Extra Copies

- Buffer is allocated once, written once
- No intermediate string concatenations
- Direct pointer passed to syscall (zero-copy)

### Future Optimization: String Interning

If print() is called frequently with the same strings:

```nebula
io::println("Processing...");
io::println("Processing...");  // Allocates same string twice
```

Could be optimized with string interning, but current design prioritizes correctness.

---

## Conclusion

The Nebula I/O subsystem demonstrates how to build safe, efficient I/O primitives within the CVT memory model. By combining:

1. **Minimal C FFI** (syscalls only)
2. **Type-safe abstraction** (Stringable trait)
3. **Generic functions** (print<T>)
4. **Automatic cleanup** (Last-Usage Analysis)

We achieve a system that is:
- **Safe:** No memory leaks or use-after-free
- **Portable:** Works on Windows, Linux, macOS
- **Efficient:** Direct syscalls, no buffering overhead
- **Extensible:** Users can implement Stringable for custom types

This design serves as a template for other Nebula standard library subsystems (file I/O, network I/O, concurrency, etc.).
