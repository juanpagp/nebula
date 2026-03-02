# Nebula Specification: String Interpolation

## 1. Overview

String interpolation in Nebula is syntactic sugar that produces a `str` View into a newly created **Region**, unless fully resolved at compile time.

Interpolation integrates directly with:

* The `str { i8*, i64 }` slice representation
* The CVT Region lifecycle model
* Deterministic last-use deallocation
* Causal capture and union rules

---

# 2. Type Model

### 2.1 `str`

```
struct str {
    i8* ptr;
    i64 len;
}
```

* `str` is a View.
* `str` does not own memory.
* The backing memory must belong to a Region or Global.

---

# 3. Syntax

```
InterpolatedString :=
    $" segment* "

segment :=
    literal_text
  | { expression }
```

Example:

```
$"Hello {name}"
$"{i}th iteration"
```

---

# 4. Semantic Rules

## 4.1 Segment Evaluation Order

Segments are evaluated left-to-right.

Expressions inside `{}` are evaluated in lexical order.

---

## 4.2 Type Requirement for `{expression}`

An expression inside `{}` must satisfy:

1. Be of type `str`, OR
2. Implement the compiler-known formatting interface:

```
i64 __format_len(T value);
void __format_write(i8* dest, T value);
```

Failure to satisfy this is a compile-time error.

---

# 5. Compile-Time Evaluation

## 5.1 Full Constant Folding

If all segments are compile-time constants:

* Evaluate interpolation at compile time.
* Emit a global constant string.
* Region state: `Global`.

Example:

```
str s = $"Hello {"World"}";
```

Lower to:

```
@.str = private constant [12 x i8] c"Hello World"
```

No Region allocation occurs.

---

## 5.2 Partial Constant Folding

Literal segments must be merged at compile time.

Example:

```
$"Hello " + {name} + "!"
```

Compiler precomputes:

* len("Hello ") = 6
* len("!") = 1

Only dynamic parts computed at runtime.

---

# 6. Runtime Lowering

If not fully constant, interpolation lowers to:

### 6.1 Length Phase

Compute total length:

```
total =
    sum(literal_lengths)
  + sum(__format_len(expr_i))
```

---

### 6.2 Region Allocation

```
Region R = region_alloc(total)
```

* R initial state: `Valid`
* R not Captured initially
* R tracked by CVT

---

### 6.3 Write Phase

Sequential write into R:

```
offset = 0

for each segment:
    if literal:
        memcpy(R.ptr + offset, literal_data, literal_len)
        offset += literal_len
    else:
        __format_write(R.ptr + offset, expr)
        offset += __format_len(expr)
```

---

### 6.4 Result Construction

Return:

```
str result = { R.ptr, total }
```

The result is a View of Region R.

---

# 7. CVT Integration

## 7.1 Region State

A Region created by interpolation:

* Starts as `Valid`
* Becomes `Captured` if stored in a parent Region
* Becomes `Global` if fully constant
* Transitions per standard CVT rules

---

## 7.2 Deterministic Deallocation

If the interpolated string:

* Is not captured
* Is not returned
* Is not stored in another Region

Then at last use:

```
free(R)
```

Is injected by CVT.

---

## 7.3 Capture Semantics

Example:

```
class A { str s; }

var a = new A();
a.s = $"Hello {name}";
```

Effect:

* Interpolation creates Region R
* Assigning to `a.s` causes:

  * R transitions to `Captured`
  * Parent = Region(A)
* R freed when final parent dies

---

## 7.4 Return Semantics

```
str f(str name) {
    return $"Hello {name}";
}
```

* Region R created inside `f`
* Returning View causes R to escape
* R remains `Valid`
* Caller becomes responsible via CVT
* R freed at last use in caller

---

# 8. Prohibited Behavior

Stack allocation (`alloca`) MUST NOT be used for interpolation Regions because:

* Stack memory is not a Region
* Cannot participate in CVT
* Cannot transition states
* Cannot be Captured
* Cannot be freed deterministically

All runtime interpolation must allocate a Region.

---

# 9. Optimization Rules

## 9.1 Allowed Optimizations

Compiler MAY:

* Fully constant-fold
* Merge adjacent literals
* Inline `__format_len`
* Inline `__format_write`
* Replace `region_alloc` with bump allocator
* Specialize small-size allocation paths

---

## 9.2 Escape-Based Optimization (Optional Advanced)

If:

* Region is proven non-Captured
* Region dies immediately after a call
* No View escapes

Backend MAY:

* Replace `region_alloc` with stack allocation
* Elide explicit free

BUT:

Semantically it must behave as a Region.

This is a backend optimization only.

---

# 10. Performance Characteristics

Runtime interpolation cost:

* O(N) copy of total bytes
* One Region allocation
* No intermediate allocations
* No temporary `str` objects

With bump allocator, allocation is O(1).

---

# 11. Formatting ABI

For each type T allowed in `{}`:

Compiler must generate or require:

```
i64 __format_len_T(T value);
void __format_write_T(i8* dest, T value);
```

For `str`:

```
__format_len_str(s) = s.len
__format_write_str(dest, s):
    memcpy(dest, s.ptr, s.len)
```

---

# 12. Example Lowering (LLVM-Level Concept)

High-level:

```
str s = $"Hello {name}";
```

Lowered conceptually:

```
len0 = 6
len1 = name.len
total = len0 + len1

R = region_alloc(total)

memcpy(R.ptr, "Hello ", 6)
memcpy(R.ptr + 6, name.ptr, name.len)

s.ptr = R.ptr
s.len = total
```

CVT handles free.

---

# 13. Safety Invariant

An interpolated string is safe iff:

```
Valid(View) ⇔ Valid(Region)
```

No View may be accessed after Region transitions to:

* Invalid
* MaybeInvalid

This is enforced by CVT.

---

# 14. Design Guarantees

This design guarantees:

* No hidden GC
* No implicit ownership transfer
* Deterministic deallocation
* No dangling stack pointers
* Full compatibility with Captured/Union rule
* Optimal single-allocation runtime behavior
* Zero-cost constant cases

---

# 15. Implementation Plan (Compiler Pipeline)

### Phase 1 — Parser

* Add InterpolatedString AST node
* Store ordered segments

---

### Phase 2 — Semantic Analysis

* Type-check expressions
* Validate formatting availability
* Mark constexpr eligibility

---

### Phase 3 — Constant Folding Pass

If all segments constexpr:

* Evaluate
* Replace AST node with StringLiteral

---

### Phase 4 — Lowering to IR

If not constant:

1. Compute total length
2. Emit `region_alloc(total)`
3. Emit sequential writes
4. Construct `{ptr,len}` struct

---

### Phase 5 — CVT Integration

* Assign Region ID to allocation
* Register View relationship
* Allow standard state transitions
* Rely on existing Last-Use injection

---

# 16. Final Architectural Conclusion

In Nebula:

* `str` is always a View.
* Interpolation always creates a Region unless constant.
* Stack memory is not part of CVT and must not be used directly.
* Performance depends on Region allocator design, not interpolation design.
