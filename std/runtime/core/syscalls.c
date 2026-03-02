#include <stdint.h>

// Forward declarations of platform-specific syscalls
// These are implemented in the platform-specific files (e.g., platform/linux_syscalls.c)
extern long sys_write(int fd, const void* buf, long count);
extern void sys_exit(int code);
extern void* sys_mmap(void* addr, unsigned long length, int prot, int flags, int fd, long offset);
extern int sys_munmap(void* addr, unsigned long length);

