#include <stdint.h>

// Forward declarations from platform-specific syscalls
extern long sys_write(int fd, const void* buf, long count);
extern void neb_panic(const uint8_t* msg_ptr, int64_t msg_len);

// ── Internal helpers ──────────────────────────────────────────────────────────

static void rt_write_bytes(const uint8_t* buf, int32_t len)
{
    sys_write(1, (const void*)buf, (long)len);
}

static int32_t rt_strlen(const uint8_t* s)
{
    int32_t n = 0;
    while (s[n] != '\0') n++;
    return n;
}

// Write a signed 64-bit integer as decimal text
static void rt_write_i64(int64_t v)
{
    if (v < 0) {
        rt_write_bytes((const uint8_t*)"-", 1);
        v = -v;
    }
    uint8_t buf[20];
    int32_t end = 19;
    buf[end] = '\0';
    if (v == 0) {
        rt_write_bytes((const uint8_t*)"0", 1);
        return;
    }
    while (v > 0) {
        buf[--end] = (uint8_t)('0' + (v % 10));
        v /= 10;
    }
    rt_write_bytes(buf + end, 19 - end);
}

// Write a floating-point value with 6 decimal places
static void rt_write_f64(double v)
{
    if (v < 0.0) {
        rt_write_bytes((const uint8_t*)"-", 1);
        v = -v;
    }
    int64_t intPart = (int64_t)v;
    rt_write_i64(intPart);
    rt_write_bytes((const uint8_t*)".", 1);
    double frac = v - (double)intPart;
    for (int i = 0; i < 6; i++) {
        frac *= 10.0;
        int digit = (int)frac;
        uint8_t ch = (uint8_t)('0' + digit);
        rt_write_bytes(&ch, 1);
        frac -= digit;
    }
}

// ── Minimal snprintf for string interpolation (no libc) ──────────────────────
// Supports: %s (char*), %d/%i (int32), %ld (int64), %g/%f (double)
// Returns number of bytes written (excluding NUL).
static int neb_snprintf(uint8_t* out, int64_t size, const uint8_t* fmt, ...)
{
    // Minimal va_list-free implementation using direct argument extraction via
    // a pointer array. Since Nebula's codegen always calls with specific types
    // we implement a simple state machine over the format string.
    //
    // We use __builtin_va_list / __builtin_va_arg (GCC/Clang built-ins).
    __builtin_va_list ap;
    __builtin_va_start(ap, fmt);

    int64_t pos = 0;
    const uint8_t* p = fmt;

    while (*p && pos < size - 1)
    {
        if (*p != '%')
        {
            out[pos++] = *p++;
            continue;
        }
        p++; // consume '%'

        // Optional 'l' modifier
        int is_long = 0;
        if (*p == 'l') { is_long = 1; p++; }

        switch (*p++)
        {
            case 'd': case 'i':
            {
                int64_t v = is_long ? __builtin_va_arg(ap, int64_t) : (int64_t)__builtin_va_arg(ap, int);
                // convert to string in tmp
                uint8_t tmp[24]; int32_t e = 23; tmp[e] = '\0';
                if (v < 0) { out[pos++] = '-'; v = -v; }
                if (v == 0) { out[pos++] = '0'; break; }
                while (v > 0 && e > 0) { tmp[--e] = (uint8_t)('0' + (v % 10)); v /= 10; }
                while (tmp[e] && pos < size - 1) out[pos++] = tmp[e++];
                break;
            }
            case 's':
            {
                const char* s = __builtin_va_arg(ap, const char*);
                if (!s) { s = "(null)"; }
                while (*s && pos < size - 1) out[pos++] = (uint8_t)*s++;
                break;
            }
            case 'g': case 'f':
            {
                double v = __builtin_va_arg(ap, double);
                if (v < 0.0) { out[pos++] = '-'; v = -v; }
                int64_t ip = (int64_t)v;
                // integer part
                uint8_t tmp[24]; int32_t e = 23; tmp[e] = '\0';
                int64_t iv = ip;
                if (iv == 0) { out[pos++] = '0'; }
                else { while (iv > 0 && e > 0) { tmp[--e] = (uint8_t)('0' + (iv % 10)); iv /= 10; } }
                while (e < 24 && tmp[e] && pos < size - 1) out[pos++] = tmp[e++];
                // fractional part (6 digits)
                if (pos < size - 1) out[pos++] = '.';
                double frac = v - (double)ip;
                for (int i = 0; i < 6 && pos < size - 1; i++) {
                    frac *= 10.0;
                    int digit = (int)frac;
                    out[pos++] = (uint8_t)('0' + digit);
                    frac -= digit;
                }
                break;
            }
            case '%': out[pos++] = '%'; break;
            default: break;
        }
    }
    out[pos] = '\0';
    __builtin_va_end(ap);
    return (int)pos;
}

// Expose snprintf as a standard symbol for the linker.
int snprintf(char* buf, int64_t size, const char* fmt, ...)
{
    __builtin_va_list ap;
    __builtin_va_start(ap, fmt);
    // Re-route through neb_snprintf by rebuilding the call with vsnprintf semantics.
    // Simplest: just call __builtin_vsnprintf isn't available without libc headers.
    // Instead, re-implement inline with the va_list.
    int64_t pos = 0;
    const char* p = fmt;
    while (*p && pos < size - 1)
    {
        if (*p != '%') { buf[pos++] = *p++; continue; }
        p++;
        int is_long = 0;
        if (*p == 'l') { is_long = 1; p++; }
        switch (*p++)
        {
            case 'd': case 'i':
            {
                int64_t v = is_long ? __builtin_va_arg(ap, int64_t) : (int64_t)__builtin_va_arg(ap, int);
                char tmp[24]; int e = 23; tmp[e] = '\0';
                int neg = (v < 0); if (neg) v = -v;
                if (v == 0) { buf[pos++] = '0'; break; }
                while (v > 0 && e > 0) { tmp[--e] = (char)('0' + (v % 10)); v /= 10; }
                if (neg && pos < size - 1) buf[pos++] = '-';
                while (tmp[e] && pos < size - 1) buf[pos++] = tmp[e++];
                break;
            }
            case 's':
            {
                const char* s = __builtin_va_arg(ap, const char*);
                if (!s) s = "(null)";
                while (*s && pos < size - 1) buf[pos++] = *s++;
                break;
            }
            case 'g': case 'f':
            {
                double v = __builtin_va_arg(ap, double);
                int neg = (v < 0); if (neg) v = -v;
                int64_t ip = (int64_t)v;
                char tmp[24]; int e = 23; tmp[e] = '\0';
                int64_t iv = ip;
                if (iv == 0) { buf[pos++] = '0'; }
                else { while (iv > 0 && e > 0) { tmp[--e] = (char)('0' + (iv % 10)); iv /= 10; } }
                if (neg && pos < size - 1) buf[pos++] = '-';
                while (e < 24 && tmp[e] && pos < size - 1) buf[pos++] = tmp[e++];
                if (pos < size - 1) buf[pos++] = '.';
                double frac = v - (double)ip;
                for (int i = 0; i < 6 && pos < size - 1; i++) {
                    frac *= 10.0; int digit = (int)frac;
                    buf[pos++] = (char)('0' + digit); frac -= digit;
                }
                break;
            }
            case '%': buf[pos++] = '%'; break;
            default: break;
        }
    }
    buf[pos] = '\0';
    __builtin_va_end(ap);
    return (int)pos;
}

// ── Public FFI surface used by test/features.neb ─────────────────────────────

// Nebula str is { i8* ptr, i64 len }
typedef struct { const uint8_t* ptr; int64_t len; } NebStr;

void write_i32(int32_t n)
{
    rt_write_i64((int64_t)n);
    rt_write_bytes((const uint8_t*)"\n", 1);
}

void write_i64(int64_t n)
{
    rt_write_i64(n);
    rt_write_bytes((const uint8_t*)"\n", 1);
}

void write_f64(double v)
{
    rt_write_f64(v);
    rt_write_bytes((const uint8_t*)"\n", 1);
}

void write_bool(int32_t b)
{
    if (b) {
        rt_write_bytes((const uint8_t*)"true\n", 5);
    } else {
        rt_write_bytes((const uint8_t*)"false\n", 6);
    }
}

void write_str(NebStr s)
{
    rt_write_bytes(s.ptr, (int32_t)s.len);
    rt_write_bytes((const uint8_t*)"\n", 1);
}

void panic_msg(NebStr s)
{
    neb_panic(s.ptr, s.len);
}
