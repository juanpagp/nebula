# Nebula: Causal Validity Tracking (CVT) Specification

### I. The Core Axiom
Nebula memory safety is governed by **Causal Validity**, not Ownership.

> **The Invariant:** A program is safe if and only if no **View** is accessed after its associated **Region** has transitioned to an `Invalid` or `MaybeInvalid` state.

---

### II. Memory Architecture
* **Regions ($R$):** Isolated heap allocations. These are the only entities that can be "freed."
* **Views ($V$):** Stack-based variables (pointers) into a Region. All variables of `class` type are Views.
* **Causal Dependencies:** If Region $A$ contains a View pointing to Region $B$, $B$ is a "Causal Child" of $A$.

---

### III. State Transition Engine
The compiler tracks these states for every Region ID at every point in the Control Flow Graph (CFG):

| State | Causal Context | Access? | Deallocation Logic |
| :--- | :--- | :--- | :--- |
| **`Valid`** | Local Stack liveness. | **Yes** | `free()` at last local use. |
| **`Captured`** | Member of another Region. | **Yes** | `free()` when the *final* Parent dies. |
| **`Global`** | Member of a Static/Global. | **Yes** | `free()` at program `exit()`. |
| **`MaybeInvalid`**| Divergent flow / `backlink`. | **No** | Requires a "Validity Proof" (e.g., `if`). |
| **`Invalid`** | Explicitly `dropped`. | **No** | Already deallocated. |

---

### IV. Hierarchical Validity & The "Shared Capture" Rule
When a View of Region $B$ is assigned to a field in Region $A$, $B$ transitions to the **Captured** state. Its lifetime is now causal to the parent.

**The Union Rule:**
If a Region is captured by multiple parents ($P_1, P_2, \dots, P_n$), its lifespan is the **Union** of those parents' lifespans. The validity of a child view $V_{child}$ is defined as:

$$Valid(V_{child}) \iff \left( \bigvee_{i=1}^{n} Valid(P_i) \right) \land \neg \text{Dropped}(R_{child})$$

The compiler inserts an automatic `free(R_child)` call only after the "Last Use" of the final surviving parent in the set.

---

### V. Breaking Cycles: The `backlink` Keyword
To allow cyclic structures (like Doubly Linked Lists) without causing "Causal Deadlocks" (where $A$ keeps $B$ alive and $B$ keeps $A$ alive):

* **`backlink`**: A View that allows access but **does not contribute to liveness analysis**.
* **Safety:** A `backlink` is treated by the compiler as `MaybeInvalid`. The developer must provide a "Validity Proof" (e.g., an existence check) to promote it to `Valid` within a specific scope.

---

### VI. FFI Boundary: `Ref<T>` and Mandatory Hinting
Because the compiler cannot perform Causal Analysis on external C code, it relies entirely on **Parameter Hinting** in `extern` declarations to maintain the safety contract.

#### 1. `Ref<T>` (The Primitive)
A private, primitive type representing a raw memory address. It lacks high-level class ergonomics and is used exclusively for FFI and low-level system wrappers.

#### 2. `keeps` (The Safe Hint)
The default assumption for FFI. It tells the CVT engine: "This function views the memory but will not free it." The View remains `Valid` after the call returns.
```java
extern "C" i32 print(keeps Ref<string> str); 

```

#### 3. `drops` (The Terminal Hint)

Tells the CVT engine: "This function consumes and deallocates the region." Upon calling, the Region and all associated Views in Nebula-land transition to `Invalid`.

```java
extern "C" void free(drops Ref<T> ref); 

```

---

### VII. Deterministic Deallocation

If a Region hasn't been explicitly `dropped`, the compiler performs **Last-Usage Analysis**:

1. Locate the final instruction in the CFG where any View of a Region is accessed.
2. Inject a `free()` call immediately following that instruction.
3. **Hierarchy Exception:** If a Region is `Captured`, the `free()` call is only injected once all Parent Regions reach their own "Last Use."

---

### VIII. The Branching & Merging Rule

When control-flow paths merge, the compiler performs a **Set Union** of states.

* If Path A = `Valid` and Path B = `Invalid`, the resulting state is `MaybeInvalid`.
* Accessing a `MaybeInvalid` region triggers a compile-time error. The user must resolve this via a proof (e.g., `if (region != null)` or a manual `assume`).

---

### IX. Example: Cyclic Safety

```java
class Parent { Child c; }
class Child { backlink Parent p; }

i32 main() {
    var p = new Parent();
    var c = new Child();
    
    p.c = c; // c is now Captured by p
    c.p = p; // p is NOT captured by c (due to backlink)
    
    return 0; 
    // 1. Last use of 'p' ends.
    // 2. Compiler inserts free(p).
    // 3. Hierarchy check: 'p' was the last parent of 'c'.
    // 4. Compiler inserts free(c).
}

```