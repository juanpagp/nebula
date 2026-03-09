#include <stdint.h>

// Forward declarations from platform-specific syscalls
extern long sys_write(int fd, const void* buf, long count);
extern void* neb_alloc(uint64_t size);

// Utility function to get length of null-terminated string
static int32_t __nebula_strlen(const uint8_t* str)
{
    int32_t len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

// Bridge to platform-specific sys_write
// Implementation delegates to the platform-specific syscall layer
void __nebula_rt_write(const uint8_t* buf, int32_t len)
{
    sys_write(1, (const void*)buf, (long)len);
}

// ---------------------------------------------------------
// Core Nebula string type
// ---------------------------------------------------------

typedef struct {
    const uint8_t* ptr;
    int64_t len;
} NebulaStr;

// Wrapper for direct string printing in Nebula
void __nebula_rt_print(NebulaStr s)
{
    __nebula_rt_write(s.ptr, (int32_t)s.len);
}

// Wrapper for printing string with a newline
void __nebula_rt_println(NebulaStr s)
{
    __nebula_rt_print(s);
    __nebula_rt_write((const uint8_t*)"\n", 1);
}

// ---------------------------------------------------------
// Primitive toString helpers (used by Stringable trait)
// ---------------------------------------------------------

// We use thread-local static buffers to avoid malloc for simple printing.
// This means the returned strings are temporary and will be overwritten
// on the next call within the same thread.
// No thread-local storage in freestanding mode without proper support
#define THREAD_LOCAL

// We define our own simple itoa to avoid libc dependency
static void reverse_str(char* str, int len) {
    int i = 0, j = len - 1;
    while (i < j) {
        char temp = str[i];
        str[i] = str[j];
        str[j] = temp;
        i++; j--;
    }
}

NebulaStr __nebula_rt_i64_to_str(int64_t v) {
    char tmp[32];
    int i = 0;
    int is_neg = 0;
    
    if (v == 0) {
        tmp[i++] = '0';
        tmp[i] = '\0';
    } else {
        if (v < 0) {
            is_neg = 1;
            v = -v;
        }
        while (v != 0) {
            tmp[i++] = (v % 10) + '0';
            v = v / 10;
        }
        if (is_neg) {
            tmp[i++] = '-';
        }
        tmp[i] = '\0';
        reverse_str(tmp, i);
    }
    uint8_t* buf = (uint8_t*)neb_alloc((uint64_t)(i + 1));
    if (!buf) return (NebulaStr){ (const uint8_t*)"", 0 };
    for (int k = 0; k <= i; k++) buf[k] = (uint8_t)tmp[k];
    return (NebulaStr){ buf, i };
}

/**
 * Format an i64 value using a printf-like format specifier string.
 * Supported formats:
 *   "000"   → zero-pad to width (width = number of zeros)
 *   "###"   → right-align with spaces to width (width = number of #)
 *   plain number string → treat as minimum width with space padding
 *
 * The format spec is passed as a NebulaStr.
 */
NebulaStr __nebula_rt_i64_to_str_fmt(int64_t v, NebulaStr fmt) {
    // Convert the integer to a plain decimal string first.
    char digits[32];
    int dlen = 0;
    int is_neg = 0;

    if (v == 0) {
        digits[dlen++] = '0';
    } else {
        int64_t tmp_v = v;
        if (tmp_v < 0) {
            is_neg = 1;
            tmp_v = -tmp_v;
        }
        while (tmp_v != 0) {
            digits[dlen++] = (tmp_v % 10) + '0';
            tmp_v /= 10;
        }
        if (is_neg) digits[dlen++] = '-';
        reverse_str(digits, dlen);
    }
    digits[dlen] = '\0';

    // Parse the format spec to determine pad character and width.
    char pad_char = ' ';
    int width = 0;

    if (fmt.len > 0) {
        // If every character is '0', use zero-padding; width = number of '0's.
        // If characters are '#', use space-padding; width = number of '#'s.
        // Otherwise try to parse as a plain integer width.
        int all_zeros = 1;
        int all_hashes = 1;
        for (int64_t i = 0; i < fmt.len; i++) {
            if (fmt.ptr[i] != '0') all_zeros = 0;
            if (fmt.ptr[i] != '#') all_hashes = 0;
        }
        if (all_zeros) {
            pad_char = '0';
            width = (int)fmt.len;
        } else if (all_hashes) {
            pad_char = ' ';
            width = (int)fmt.len;
        } else {
            // Parse as integer width
            for (int64_t i = 0; i < fmt.len; i++) {
                uint8_t c = fmt.ptr[i];
                if (c >= '0' && c <= '9')
                    width = width * 10 + (c - '0');
            }
        }
    }

    // Compute the total output length.
    int out_len = dlen;
    if (width > dlen) out_len = width;

    uint8_t* buf = (uint8_t*)neb_alloc((uint64_t)(out_len + 1));
    if (!buf) return (NebulaStr){ (const uint8_t*)"", 0 };

    int pad = out_len - dlen;
    int offset = 0;

    if (pad_char == '0' && is_neg) {
        // "-000123": sign first, then zeros, then digits (minus already in digits)
        // digits[0] is '-', rest are the number
        buf[offset++] = '-';
        for (int p = 0; p < pad; p++) buf[offset++] = '0';
        // skip the '-' in digits (already written)
        for (int d = 1; d < dlen; d++) buf[offset++] = (uint8_t)digits[d];
    } else {
        for (int p = 0; p < pad; p++) buf[offset++] = (uint8_t)pad_char;
        for (int d = 0; d < dlen; d++) buf[offset++] = (uint8_t)digits[d];
    }
    buf[out_len] = '\0';
    return (NebulaStr){ buf, out_len };
}

NebulaStr __nebula_rt_u64_to_str(uint64_t v) {
    char tmp[32];
    int i = 0;
    
    if (v == 0) {
        tmp[i++] = '0';
        tmp[i] = '\0';
    } else {
        while (v != 0) {
            tmp[i++] = (v % 10) + '0';
            v = v / 10;
        }
        tmp[i] = '\0';
        reverse_str(tmp, i);
    }
    uint8_t* buf = (uint8_t*)neb_alloc((uint64_t)(i + 1));
    if (!buf) return (NebulaStr){ (const uint8_t*)"", 0 };
    for (int k = 0; k <= i; k++) buf[k] = (uint8_t)tmp[k];
    return (NebulaStr){ buf, i };
}

// Float is harder without libc, so we do a very basic approximation 
// for display purposes: integer part + 6 decimal places.
NebulaStr __nebula_rt_f64_to_str(double v) {
    char tmp[64];
    int i = 0;
    
    if (v < 0) {
        tmp[i++] = '-';
        v = -v;
    }
    
    int64_t int_part = (int64_t)v;
    double frac_part = v - (double)int_part;
    
    // Convert int part
    char int_buf[32];
    int int_len = 0;
    if (int_part == 0) {
        int_buf[int_len++] = '0';
    } else {
        while (int_part != 0) {
            int_buf[int_len++] = (int_part % 10) + '0';
            int_part /= 10;
        }
    }
    
    // Reverse int part and copy to main buf
    for (int j = int_len - 1; j >= 0; j--) {
        tmp[i++] = int_buf[j];
    }
    
    // Decimal point
    tmp[i++] = '.';
    
    // 6 decimal places
    for (int d = 0; d < 6; d++) {
        frac_part *= 10;
        int digit = (int)frac_part;
        tmp[i++] = digit + '0';
        frac_part -= digit;
    }
    
    tmp[i] = '\0';
    uint8_t* buf = (uint8_t*)neb_alloc((uint64_t)(i + 1));
    if (!buf) return (NebulaStr){ (const uint8_t*)"", 0 };
    for (int k = 0; k <= i; k++) buf[k] = (uint8_t)tmp[k];
    return (NebulaStr){ buf, i };
}

NebulaStr __nebula_rt_bool_to_str(int32_t v) {
    if (v) {
        return (NebulaStr){ (const uint8_t*)"true", 4 };
    } else {
        return (NebulaStr){ (const uint8_t*)"false", 5 };
    }
}

// ---------------------------------------------------------
// String interpolation helper (used by $"..." expressions)
// ---------------------------------------------------------

// Concatenate N NebulaStr values into a single heap-allocated buffer.
NebulaStr __nebula_rt_str_concat(NebulaStr* parts, int64_t count)
{
    int64_t total_len = 0;
    for (int64_t i = 0; i < count; i++)
    {
        total_len += parts[i].len;
    }

    uint8_t* buf = (uint8_t*)neb_alloc((uint64_t)(total_len + 1));
    if (!buf)
    {
        return (NebulaStr){ (const uint8_t*)"", 0 };
    }

    int64_t offset = 0;
    for (int64_t i = 0; i < count; i++)
    {
        for (int64_t j = 0; j < parts[i].len; j++)
        {
            buf[offset++] = parts[i].ptr[j];
        }
    }
    buf[total_len] = '\0';

    return (NebulaStr){ buf, total_len };
}

// ---------------------------------------------------------
// Structural toStr helpers for tuples and arrays
// ---------------------------------------------------------

/**
 * Build a string of the form "[elem0, elem1, ...]" from an array of already-
 * converted NebulaStr parts.  Ownership of the input parts array stays with
 * the caller (nothing is freed here).
 */
NebulaStr __nebula_rt_format_array_str(const NebulaStr* elements, int64_t count)
{
    // Total length: "["(1) + elements + ", " between each pair + "]"(1)
    int64_t total_len = 2; // '[' + ']'
    for (int64_t i = 0; i < count; i++)
    {
        total_len += elements[i].len;
        if (i > 0) total_len += 2; // ", "
    }

    uint8_t* buf = (uint8_t*)neb_alloc((uint64_t)(total_len + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    int64_t off = 0;
    buf[off++] = '[';
    for (int64_t i = 0; i < count; i++)
    {
        if (i > 0) { buf[off++] = ','; buf[off++] = ' '; }
        for (int64_t j = 0; j < elements[i].len; j++)
            buf[off++] = elements[i].ptr[j];
    }
    buf[off++] = ']';
    buf[off]   = '\0';

    return (NebulaStr){ buf, total_len };
}

// ---------------------------------------------------------
// Entry point: argc/argv -> Nebula str[]
// ---------------------------------------------------------

/**
 * Converts a C argc/argv pair into a heap-allocated array of NebulaStr values.
 * The array is tightly packed with exactly argc elements.  The companion length
 * (argc) is passed back separately through the __nebula_build_argv_count global
 * so the compiler-generated main prologue can populate a runtime-length alloca
 * alongside the data pointer.
 *
 * Called by the compiler-generated main prologue when the Nebula entry point
 * is declared as  main(str[] args).
 */
NebulaStr* __nebula_build_argv(int32_t argc, const uint8_t** argv)
{
    NebulaStr* result = (NebulaStr*)neb_alloc((uint64_t)argc * (uint64_t)sizeof(NebulaStr));
    if (!result) return result;
    for (int32_t i = 0; i < argc; i++)
    {
        const uint8_t* s = argv[i];
        int64_t len = 0;
        while (s[len] != '\0') { len++; }
        result[i] = (NebulaStr){ s, len };
    }
    return result;
}

// ---------------------------------------------------------
// str equality
// ---------------------------------------------------------

/**
 * Returns 1 if the two Nebula strings contain identical bytes, 0 otherwise.
 * Both strings are passed by value (two-register SysV ABI: { ptr, i64 }).
 * Used by the compiler when it lowers  str == str  and  str != str.
 */
int32_t __nebula_rt_str_eq(NebulaStr a, NebulaStr b)
{
    if (a.len != b.len) return 0;
    for (int64_t i = 0; i < a.len; i++)
        if (a.ptr[i] != b.ptr[i]) return 0;
    return 1;
}
