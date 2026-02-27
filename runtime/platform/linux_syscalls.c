#include <stdint.h>

long sys_write(int fd, const void* buf, long count) {
    long ret;
    __asm__ volatile (
        "syscall"
        : "=a" (ret)
        : "a" (1), "D" (fd), "S" (buf), "d" (count)
        : "rcx", "r11", "memory"
    );
    return ret;
}

void sys_exit(int code) {
    __asm__ volatile (
        "syscall"
        :
        : "a" (60), "D" (code)
        : "rcx", "r11", "memory"
    );
}

void* sys_mmap(void* addr, unsigned long length, int prot, int flags, int fd, long offset) {
    void* ret;
    long l_flags = flags;
    long l_fd = fd;
    long l_offset = offset;
    __asm__ volatile (
        "movq %5, %%r10\n\t"
        "movq %6, %%r8\n\t"
        "movq %7, %%r9\n\t"
        "syscall"
        : "=a" (ret)
        : "a" (9), "D" (addr), "S" (length), "d" (prot), "r" (l_flags), "r" (l_fd), "r" (l_offset)
        : "rcx", "r11", "r10", "r8", "r9", "memory"
    );
    return ret;
}

int sys_munmap(void* addr, unsigned long length) {
    int ret;
    __asm__ volatile (
        "syscall"
        : "=a" (ret)
        : "a" (11), "D" (addr), "S" (length)
        : "rcx", "r11", "memory"
    );
    return ret;
}
