# `nebc`: The Nebula Compiler

The Nebula Compiler (`nebc`) is the official compiler for the **Nebula** Systems Programming Language. It transforms Nebula source code into native binaries, prioritizing performance, safety, and deterministic memory management.

-----

## The Nebula Language

Nebula is a modern **Systems Object-Oriented Programming (OOP)** language designed for applications requiring high-level control and zero-overhead abstractions, that doesn't sacrifice readability and user experience.

**Key Design Attributes:**

* **Safety & Modernity:** Nebula is fundamentally **null-free** and **exception-free**. It enforces explicit error handling through static, compile-time checked `Result<T, E>` and `Option<T>` tagged unions, eliminating entire classes of runtime errors.
* **Memory Safety:** Nebula achieves guaranteed memory safety without relying on a non-deterministic Garbage Collector (GC). It employs a hybrid, multi-pass static analysis model—including **Escape Analysis**, **Ownership Analysis**, and an optional, compile-time **Borrow Checker**—to insert deterministic memory deallocation calls. The only fallback is a minimal, low-overhead Automatic Reference Counting (ARC) system for instances with dynamically unknown lifetimes.
* **Efficiency:** The language guarantees **zero-cost abstractions**. High-level syntactic features are resolved during the AST lowering phase, ensuring that no runtime overhead is introduced.

-----

## Compiler Implementation and Technology Stack

The `nebc` compiler is built for speed and stability using a modern Java ecosystem and is compiled to a native executable.

### Technology Stack

| Component | Technology | Role |
| :--- | :--- | :--- |
| **Implementation** | Java (JDK 23+) | Core logic, AST manipulation, and analysis passes. |
| **Compiler Host** | **GraalVM Native Image** | Compiles the `nebc` application itself to a self-contained native executable. |
| **Frontend** | ANTLR4 | Responsible for generating the Lexer and Parser for the initial transformation of source code into the Parse Tree. |
| **Backend** | LLVM | The industry-standard framework used for high-level Intermediate Representation (IR) generation, optimization, and target-specific code emission. |
| **LLVM Bridge** | JavaCPP Presets | Provides robust JNI/JNA bindings to the native LLVM C API, allowing direct, programmatic IR construction from within the Java environment. |

### Compilation Pipeline

The compilation process is executed across four distinct, sequential phases:

1.  **Lexing & Parsing:** Converts source files into an Abstract Syntax Tree (AST) via ANTLR.
2.  **Static Analysis:** Executes Semantic Analysis, Type Checking, and **AST Lowering** (zero-cost abstraction resolution).
3.  **Memory Analysis:** Performs **Escape Analysis** and **Ownership/Borrow Checking** to resolve memory lifetimes statically.
4.  **Code Generation:** The final AST is translated into LLVM IR, which is then passed through LLVM optimization passes, compiled to object files, and linked into the final executable.

-----

## Nebula Language Example

The following code demonstrates key language features, including Traits, Structs, Operator Overloading, and Tagged Unions:

```nebula
// Alias
alias Console.println = println;

// Constants
const float PI = 3.14f;

// Single-method trait for rendering
trait void Renderable.render() => println("Default render");

// Base class: Shape inherits Renderable
class Shape : Renderable
{
    Vec2[] vertices;
    Vec2 pos;

    // Use the overloaded operator '+'
    void move(Vec2 delta) { pos += delta; } // mutates state

    float getArea(); // abstract method to be implemented by subclasses
}

// Vector type (stack-allocated)
struct Vec2
{
    float x, y;

    // Overload the '+' operator
    operator+(Vec2 other)
    {
       return Vec2(this.x + other.x, this.y + other.y);
    }
}

// Circle subclass
struct Circle : Shape
{
    float r;
    int8 resolution;

    // Constructor with default values
    Circle(float r = 1, Vec2 pos = Vec2(0,0), int8 resolution = 30)
    {
        this.r = r;
        this.pos = pos;
        this.resolution = resolution;

        var cvOpt = calculateVerticesPositions(this);
        this.vertices = cvOpt.unwrapOrElse([]); // explicit Option handling
    }

    float getArea() => PI * r ** 2;

    override void render()
    {
        println("Rendering Circle with " + resolution + " vertices");
    }
}

// Helper function demonstrating exhaustive pattern matching over subtypes
Option<Vec2[]> calculateVerticesPositions(T: Shape shape)
{
    match shape
    {
        shape is Rect rect
        {
            Vec2[4] vertices;
            // ... vertex math ...
            return Some(vertices);
        }

        shape is Circle circle
        {
            Vec2[circle.resolution] vertices;
            // ... vertex math ...
            return Some(vertices);
        }
        // Compiler guarantees exhaustiveness
    }
}

// Tagged unions for safe alternatives
tagged union Option<T>
{
    Some(T),
    None
}

void main()
{
    // Simplified 'for' loop: a zero-cost abstraction
    for (i < 100)
    {
       var circle = Circle(pos: Vec2(5,1));
       var rect = Rect(2,2);
       var floor = Rect(h: 1, w: 5, pos: Vec2(0,20));

       Shape[] shapes = [circle, rect, floor];

       foreach (var shape in shapes)
       {
          shape.render();
          println($"Rendered {typeof(shape).toLowerCase()} with area {shape.getArea()}");
       }
    }
}
```