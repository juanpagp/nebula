/**
 * std/runtime/core/fn.c
 *
 * Typed dispatch shims for std::fn::FnRef.
 *
 * Each function receives a raw void* (the function pointer stored in an FnRef),
 * casts it to the appropriate concrete function-pointer type, and invokes it.
 * All shims are null-safe: a NULL raw pointer is a no-op / returns a zero value.
 *
 * CVT note: compiled functions reside permanently in the binary's .text section.
 * They are "Global" in CVT terms — they are never allocated and never freed, so
 * no region tracking is required for FnRef values.
 *
 * Naming convention: __nebula_rt_fn_call_<ret>[_<arg0type>[_<arg1type>...]]
 */

#include <stdint.h>

/* The Nebula str ABI: fat pointer { const uint8_t*, int64_t }. */
typedef struct
{
    const uint8_t* ptr;
    int64_t        len;
} NebulaStr;

/* =========================================================================
 * Null-check helper
 * ========================================================================= */

int32_t __nebula_rt_fn_is_valid(void* raw)
{
    return raw != (void*)0;
}

/* =========================================================================
 * Void return — zero arguments
 * ========================================================================= */

void __nebula_rt_fn_call_void(void* raw)
{
    if (!raw) return;
    ((void (*)(void))raw)();
}

/* =========================================================================
 * Void return — one argument
 * ========================================================================= */

void __nebula_rt_fn_call_void_i64(void* raw, int64_t a)
{
    if (!raw) return;
    ((void (*)(int64_t))raw)(a);
}

void __nebula_rt_fn_call_void_bool(void* raw, int32_t a)
{
    if (!raw) return;
    ((void (*)(int32_t))raw)(a);
}

void __nebula_rt_fn_call_void_str(void* raw, NebulaStr a)
{
    if (!raw) return;
    ((void (*)(NebulaStr))raw)(a);
}

void __nebula_rt_fn_call_void_f64(void* raw, double a)
{
    if (!raw) return;
    ((void (*)(double))raw)(a);
}

/* =========================================================================
 * Value return — zero arguments
 * ========================================================================= */

int64_t __nebula_rt_fn_call_i64(void* raw)
{
    if (!raw) return 0;
    return ((int64_t (*)(void))raw)();
}

/* bool is i32 at the C ABI boundary (Nebula lowers bool to i1, but C callers
   use int; the result is truncated to i1 at the Nebula call site).            */
int32_t __nebula_rt_fn_call_bool(void* raw)
{
    if (!raw) return 0;
    return ((int32_t (*)(void))raw)();
}

NebulaStr __nebula_rt_fn_call_str(void* raw)
{
    if (!raw) return (NebulaStr){ (const uint8_t*)"", 0 };
    return ((NebulaStr (*)(void))raw)();
}

double __nebula_rt_fn_call_f64(void* raw)
{
    if (!raw) return 0.0;
    return ((double (*)(void))raw)();
}
