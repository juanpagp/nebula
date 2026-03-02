# Nebula Specification: Causal Validity Tracking (CVT)

## 1. The Core Philosophy

Nebula memory safety is governed by **Causality**, not Ownership.
Safety is defined as the prevention of **Temporal Memory Violations**: a program is valid if and only if no **View** is accessed after its causal source (the **Region**) has been deallocated or invalidated.

---

## 2. Type System: Classes vs. Structs

Nebula distinguishes strictly between **Memory Regions** and **Aggregate Proxies**.

### 2.1 Classes (Reference Types)

* **Definition:** A `class` is a **View** into a heap-allocated **Region**.
* **Allocation:** Created via the `new` keyword.
* **Identity:** Two class variables can point to the same Region (aliasing).
* **Lifecycle:** Managed entirely by the CVT State Engine.

### 2.2 Structs (Value Types)

* **Definition:** A `struct` is a **Data Aggregate**. It is allocated on the stack (or inline as a field within a Class Region).
* **Allocation:** Created via direct initialization (`Var v = Var()`).
* **Identity:** Copying a struct performs a shallow copy of its members.
* **CVT Role:** Structs are **Aggregate Proxies**. If a struct contains a class field (a View), the struct variable itself becomes a carrier for that Region's validity.

---

## 3. The CVT State Engine

The compiler maintains a **State Table** for every Region ID ($R$) at every point in the Control Flow Graph (CFG).

### 3.1 Region States

| State | Definition | Access Allowed? |
| --- | --- | --- |
| **Valid** | The Region is live on the local stack. | Yes |
| **Captured** | The Region has been assigned as a field to another Region (The Parent). | Yes |
| **Global** | The Region is attached to a static or global root. | Yes |
| **MaybeInvalid** | State is ambiguous due to branching or a `backlink`. | **No** (Requires Proof) |
| **Invalid** | The Region has been dropped or its liveness has expired. | No |

### 3.2 The State Union Rule (Branching)

When two control flow paths merge (e.g., after an `if/else`), the compiler performs a set union on the states of each Region:


$$\text{State}(R_{\text{merged}}) = \text{State}(R_{\text{pathA}}) \cup \text{State}(R_{\text{pathB}})$$

* If `Valid` $\cup$ `Invalid` $\to$ `MaybeInvalid`.
* Accessing a `MaybeInvalid` Region results in a compile-time error unless guarded by a **Validity Proof** (e.g., `if (obj != null)`).

---

## 4. Causal Mechanics & Hierarchy

### 4.1 The Capture Rule

When a View of Region $B$ is assigned to a field within Region $A$, $B$ transitions to the **Captured** state.

* $A$ is now the **Causal Parent** of $B$.
* $B$ cannot be freed as long as $A$ is `Valid`.

### 4.2 The Multi-Parent Union

If $B$ is captured by multiple parents $\{P_1, P_2, \dots, P_n\}$, its validity is the logical OR of its parents' validity:


$$\text{Valid}(V_B) \iff \left( \bigvee_{i=1}^{n} \text{Valid}(P_i) \right) \wedge \neg \text{Dropped}(R_B)$$

### 4.3 Breaking Cycles: `backlink`

A `backlink` is a View that **does not** establish a causal parent-child relationship.

* **Safety:** A `backlink` is treated as `MaybeInvalid`.
* **Promotion:** To use a `backlink`, the developer must "promote" it within a scope using an existence check. This prevents "Causal Deadlocks" (leaks) where two objects keep each other alive indefinitely.

---

## 5. Deterministic Deallocation (LUA)

Nebula does not use a Garbage Collector or Reference Counting. It uses **Last-Usage Analysis (LUA)**.

1. **Liveness Mapping:** The compiler identifies the final instruction in the CFG where a View of Region $R$ (or any Aggregate Proxy containing a View of $R$) is accessed.
2. **Injection:** A `free(R)` call is injected immediately following that instruction.
3. **Hierarchy Check:** If $R$ is `Captured`, the `free(R)` call is deferred until the "Last Use" of its final remaining Parent Region.

---

## 6. The FFI Boundary & Parameter Hinting

Since C code is opaque to the Nebula compiler, hints are used to extend the Causal Chain into external binaries.

### 6.1 Primitive Types

* **`Ref<T>`**: The raw memory address. Used for low-level pointer arithmetic and FFI.

### 6.2 Mandatory Hints

* **`keeps`**: The function reads the memory but does not claim it. The Region remains `Valid` after the call.
* **`drops`**: (Default) The function consumes the memory. The Region transitions to `Invalid` in Nebula immediately after the call.
* **`mutates`**: The function may alter the data. Used for concurrency analysis.

The default is `drops` so that it forces users to properly annotate.

---

## 7. Standard Library Primitives

### 7.1 The `str` (String) Struct

The Nebula string is an **Aggregate Proxy** designed for zero-cost FFI.

```cpp
struct str {
    char[] data; // View into a heap Region
    u64 len;     // Metadata
    
    // Slicing: Returns a new struct pointing to the SAME region.
    str slice(u64 start, u64 length) => str(this.data + start, length);
}

```

* **Safety:** As long as any `str` (the original or a slice) is live on the stack, the `char[]` Region is `Valid`.
* **FFI:** Passes to C as a simple 16-byte structure (Pointer + Length).

### 7.2 Arrays

* **Fixed Arrays (`[T; N]`)**: Allocated inline (on stack or within class). No CVT tracking required for the array itself.
* **Dynamic Arrays (`char[]`)**: These are Views. The CVT engine tracks the underlying allocation Region. Pointer arithmetic is allowed but stays within the bounds of the Region's liveness.

---

## 8. Summary of Edge Cases

* **Reassignment:** If a View `v` to $R_1$ is reassigned to $R_2$, the compiler performs LUA on $R_1$ immediately. If `v` was the last view to $R_1$, $R_1$ is freed at the point of reassignment.
* **Escaping via Return:** If a function returns a View or a Struct containing a View, the LUA "Last Use" is moved to the caller's scope.
* **Nested Structs:** CVT flattens the dependency. A struct `Outer` containing a struct `Inner` containing a `ClassView` is simply tracked as a single proxy for the underlying Region.
