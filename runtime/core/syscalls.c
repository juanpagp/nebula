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
