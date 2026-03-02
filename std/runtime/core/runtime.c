#include <stdint.h>

// Forward declarations from platform-specific syscalls
extern long sys_write(int fd, const void* buf, long count);

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

// Wrapper for direct string printing in Nebula
void __nebula_rt_print(const uint8_t* buf)
{
    int32_t len = __nebula_strlen(buf);
    __nebula_rt_write(buf, len);
}

// Wrapper for printing string with a newline
void __nebula_rt_println(const uint8_t* buf)
{
    __nebula_rt_print(buf);
    __nebula_rt_write((const uint8_t*)"\n", 1);
}

// ---------------------------------------------------------
// Primitive toString helpers (used by Displayable trait)
// ---------------------------------------------------------

typedef struct {
    const uint8_t* ptr;
    int64_t len;
} NebulaStr;

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
    static THREAD_LOCAL char buf[32];
    int i = 0;
    int is_neg = 0;
    
    if (v == 0) {
        buf[i++] = '0';
        buf[i] = '\0';
        return (NebulaStr){ (const uint8_t*)buf, 1 };
    }
    
    if (v < 0) {
        is_neg = 1;
        v = -v;
    }
    
    while (v != 0) {
        buf[i++] = (v % 10) + '0';
        v = v / 10;
    }
    
    if (is_neg) {
        buf[i++] = '-';
    }
    
    buf[i] = '\0';
    reverse_str(buf, i);
    
    return (NebulaStr){ (const uint8_t*)buf, i };
}

NebulaStr __nebula_rt_u64_to_str(uint64_t v) {
    static THREAD_LOCAL char buf[32];
    int i = 0;
    
    if (v == 0) {
        buf[i++] = '0';
        buf[i] = '\0';
        return (NebulaStr){ (const uint8_t*)buf, 1 };
    }
    
    while (v != 0) {
        buf[i++] = (v % 10) + '0';
        v = v / 10;
    }
    
    buf[i] = '\0';
    reverse_str(buf, i);
    
    return (NebulaStr){ (const uint8_t*)buf, i };
}

// Float is harder without libc, so we do a very basic approximation 
// for display purposes: integer part + 6 decimal places.
NebulaStr __nebula_rt_f64_to_str(double v) {
    static THREAD_LOCAL char buf[64];
    int i = 0;
    
    if (v < 0) {
        buf[i++] = '-';
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
        buf[i++] = int_buf[j];
    }
    
    // Decimal point
    buf[i++] = '.';
    
    // 6 decimal places
    for (int d = 0; d < 6; d++) {
        frac_part *= 10;
        int digit = (int)frac_part;
        buf[i++] = digit + '0';
        frac_part -= digit;
    }
    
    buf[i] = '\0';
    return (NebulaStr){ (const uint8_t*)buf, i };
}

NebulaStr __nebula_rt_bool_to_str(int32_t v) {
    if (v) {
        return (NebulaStr){ (const uint8_t*)"true", 4 };
    } else {
        return (NebulaStr){ (const uint8_t*)"false", 5 };
    }
}
