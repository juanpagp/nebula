#include <stdint.h>

// Forward declarations of platform-specific syscalls
long sys_write(int fd, const void* buf, long count);

/**
 * __nebula_rt_write
 * Bridge for std::sys::__nebula_rt_write
 */
void __nebula_rt_write(const void* buf, int32_t len) {
    sys_write(1, buf, (long)len); // 1 is stdout
}

// Utility function to get length of null-terminated string
static int32_t __nebula_strlen(const uint8_t* str) {
    int32_t len = 0;
    while (str[len] != '\0') {
        len++;
    }
    return len;
}

// Wrapper for direct string printing in Nebula
void __nebula_rt_print(const uint8_t* buf) {
    int32_t len = __nebula_strlen(buf);
    __nebula_rt_write(buf, len);
}

// Wrapper for printing string with a newline
void __nebula_rt_println(const uint8_t* buf) {
    __nebula_rt_print(buf);
    __nebula_rt_write((const void*)"\n", 1);
}
