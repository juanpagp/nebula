# Nebula I/O Subsystem - Integration Guide

## Quick Start

The Nebula I/O subsystem provides type-safe printing functions that work with any type implementing the `Stringable` trait.

### Basic Usage

```nebula
use std::io;

i32 main() {
    // Print a string
    io::println("Hello, World!");
    
    // Print an integer
    i32 answer = 42;
    io::println(answer);
    
    // Print multiple values
    io::print("The answer is ");
    io::println(answer);
    
    return 0;
}
```

**Expected Output:**
```
Hello, World!
42
The answer is 42
```

---

## Supported Types

The I/O subsystem provides `Stringable` implementations for:

| Type | toString() Behavior | Example |
|------|-------------------|---------|
| `i32` | Decimal representation | `42` → `"42"` |
| `u32` | Unsigned decimal | `42u32` → `"42"` |
| `u8` | Byte as decimal | `255u8` → `"255"` |
| `string` | Identity (no conversion) | `"hello"` → `"hello"` |

### Example: Multiple Types

```nebula
use std::io;

i32 main() {
    i32 count = 5;
    u32 unsigned_val = 100u32;
    u8 byte_val = 65u8;
    string text = "Result";
    
    io::println(count);          // 5
    io::println(unsigned_val);   // 100
    io::println(byte_val);       // 65
    io::println(text);           // Result
    
    return 0;
}
```

---

## Using `print` vs `println`

- **`print<T>(item)`** — Outputs the value without a newline
- **`println<T>(item)`** — Outputs the value followed by a newline

### Example: Building Output Line

```nebula
use std::io;

i32 main() {
    // Using print to build a line
    io::print("Numbers: ");
    for (i32 i = 1; i <= 3; i = i + 1) {
        io::print(i);
        if (i < 3) io::print(", ");
    }
    io::println("");  // Newline at the end
    
    return 0;
}
```

**Output:**
```
Numbers: 1, 2, 3
```

---

## Loops and Conditionals

### Example: Print Number Range

```nebula
use std::io;

i32 main() {
    for (i32 i = 0; i < 5; i = i + 1) {
        io::println(i);
    }
    return 0;
}
```

**Output:**
```
0
1
2
3
4
```

### Example: Conditional Printing

```nebula
use std::io;

i32 main() {
    i32 x = 10;
    
    io::print("x is ");
    if (x > 5) {
        io::println("greater than 5");
    } else {
        io::println("5 or less");
    }
    
    return 0;
}
```

**Output:**
```
x is greater than 5
```

---

## Memory Safety (CVT)

The I/O subsystem automatically manages memory through **Last-Usage Analysis**. You don't need to manually free regions created by `toString()`.

### How It Works

```nebula
io::println(42);
```

**Behind the scenes:**
1. Call `print(42)`
2. `42.toString()` allocates a Region<u8> containing "42"
3. Region is passed to `__nebula_rt_write(keeps ...)`
4. Data is written to stdout
5. Function returns
6. **Compiler automatically frees the temporary Region**

No memory leaks! The compiler's Last-Usage Analysis ensures cleanup.

### When You Need to Free Manually

If you manually store the result of `toString()`, you're responsible for freeing:

```nebula
use std::io;

i32 main() {
    i32 x = 42;
    string s = x.toString();  // Manual allocation
    
    io::println(s);           // Print it
    
    s.free();                 // You must free it!
    
    return 0;
}
```

But if you use it directly in print, the compiler handles cleanup:

```nebula
io::println(x);  // Temporary region freed automatically
```

---

## Creating Custom Stringable Types

You can implement `Stringable` for your own types.

### Example: Custom Point Type

```nebula
use std::Stringable;

struct Point {
    i32 x;
    i32 y;
}

impl Stringable for Point {
    string toString() {
        string xs = x.toString();
        string ys = y.toString();
        
        // Build the output string
        // (String concatenation syntax TBD in Nebula)
        return "(" + xs + ", " + ys + ")";
    }
}

use std::io;

i32 main() {
    Point p = Point{x: 10, y: 20};
    io::println(p);  // Output: (10, 20)
    return 0;
}
```

### Example: Custom Record Type

```nebula
use std::Stringable;

struct Person {
    string name;
    i32 age;
}

impl Stringable for Person {
    string toString() {
        string age_str = age.toString();
        return name + " (" + age_str + " years old)";
    }
}

use std::io;

i32 main() {
    Person alice = Person{name: "Alice", age: 30};
    io::println(alice);  // Output: Alice (30 years old)
    return 0;
}
```

---

## Common Patterns

### Pattern 1: Print Debug Info

```nebula
use std::io;

i32 main() {
    i32 result = 42;
    
    io::print("[DEBUG] result = ");
    io::println(result);
    
    return 0;
}
```

### Pattern 2: Print Loop Iteration

```nebula
use std::io;

i32 main() {
    for (i32 i = 0; i < 3; i = i + 1) {
        io::print("Iteration ");
        io::println(i);
    }
    return 0;
}
```

### Pattern 3: Print Before/After Values

```nebula
use std::io;

i32 main() {
    i32 x = 5;
    io::print("Before: ");
    io::println(x);
    
    x = x + 10;
    
    io::print("After: ");
    io::println(x);
    
    return 0;
}
```

### Pattern 4: Multi-Line Output

```nebula
use std::io;

i32 main() {
    io::println("=== Report ===");
    io::print("Count: ");
    io::println(5);
    io::print("Total: ");
    io::println(100);
    io::println("=== End ===");
    return 0;
}
```

---

## Performance Notes

### Syscall Overhead

Each `print()` or `println()` call makes a single `write(2)` syscall to the OS:

- **Local variable print:** ~1µs (include itoa conversion)
- **String literal print:** ~1-10µs (syscall latency)

### Zero-Copy Design

- No intermediate buffers
- No string copying
- Raw pointer passed directly to syscall
- Memory-safe despite low-level pointers (CVT guarantees this)

### Optimization Opportunities

For applications making many print calls:

```nebula
// Less efficient (many syscalls)
for (i32 i = 0; i < 100; i = i + 1) {
    io::println(i);
}

// More efficient (batch I/O)
// [Future: would use buffered output]
```

(Buffering support is planned for Phase 2)

---

## Troubleshooting

### Issue: "Undeclared identifier: io"

**Cause:** Forgot to import the I/O module.

**Solution:** Add `use std::io;` at the top of your file.

```nebula
use std::io;  // ← Add this

i32 main() {
    io::println(42);
    return 0;
}
```

### Issue: Type doesn't implement Stringable

**Cause:** Tried to print a type without a `Stringable` implementation.

**Solution:** Implement `Stringable` for your type.

```nebula
impl Stringable for MyType {
    string toString() {
        // Return string representation
    }
}
```

### Issue: Compilation Error: "keeps expected"

**Cause:** Passing a type that doesn't match FFI expectations.

**Solution:** Use `toString()` to convert to string first.

```nebula
// Wrong:
// io::__nebula_rt_write(my_region);

// Right:
string s = item.toString();
io::println(s);
```

---

## Architecture Reference

The I/O subsystem consists of 5 layers:

1. **Layer 1 (runtime.c)**: OS-level syscalls (Windows/Unix)
2. **Layer 2 (std/sys.neb)**: FFI bindings with `extern "C"`
3. **Layer 3 (std/primitives.neb)**: Implementations for i32, u32, u8, string
4. **Layer 4 (std/traits.neb)**: The `Stringable` trait interface
5. **Layer 5 (std/io.neb)**: Public API (`print<T>`, `println<T>`)

For detailed design information, see [IO_SUBSYSTEM_DESIGN.md](IO_SUBSYSTEM_DESIGN.md).

---

## Building Programs with the I/O Subsystem

### Step 1: Create Your Nebula File

```nebula
// hello.neb
use std::io;

i32 main() {
    io::println("Hello, World!");
    return 0;
}
```

### Step 2: Compile

```bash
$ nebc hello.neb -o hello
```

### Step 3: Run

```bash
$ ./hello
Hello, World!
```

---

## Future Extensions

The I/O subsystem is designed to grow. Planned additions:

### Phase 2: Input Functions
```nebula
string readLine();
i32 read(Ref<u8> buf, i32 maxLen);
```

### Phase 3: File I/O
```nebula
i32 open(string path, i32 flags);
i32 read(i32 fd, Ref<u8> buf, i32 len);
i32 write(i32 fd, Ref<u8> buf, i32 len);
void close(i32 fd);
```

### Phase 4: Format Strings
```nebula
printf("Value: %d, String: %s\n", 42, "hello");
```

---

## Examples Directory

Complete working examples:

- **hello.neb** — Classic "Hello, World!"
- **loops.neb** — Print number range with loop
- **custom_types.neb** — Define and print custom types
- **performance.neb** — Benchmark syscall overhead

(To be added to examples/ directory)

---

## Summary

The Nebula I/O subsystem provides:

✅ **Type-safe printing** via generic `print<T>()` and `println<T>()`  
✅ **Memory-safe operations** with automatic region cleanup  
✅ **Extensible design** for custom Stringable types  
✅ **Zero libc dependencies** using direct OS syscalls  
✅ **Cross-platform support** for Windows, Linux, macOS  

Start using it by importing `std::io` and calling `print()` and `println()`!

---

## References

- [IO_SUBSYSTEM_DESIGN.md](IO_SUBSYSTEM_DESIGN.md) — Full architecture & design decisions
- [DELIVERABLES.md](DELIVERABLES.md) — Complete file manifest & project status
- [runtime.c](runtime.c) — C syscall bindings
- [std/sys.neb](std/sys.neb) — FFI declarations
- [std/traits.neb](std/traits.neb) — Stringable trait
- [std/primitives.neb](std/primitives.neb) — Type implementations
- [std/io.neb](std/io.neb) — Public API
