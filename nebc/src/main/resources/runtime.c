#include <stdint.h>

#ifdef _WIN32
// Windows raw syscalls/API without libc
// We need to declare some basic types and the WriteFile function
typedef void* HANDLE;
typedef int BOOL;
typedef unsigned long DWORD;
typedef struct _OVERLAPPED* LPOVERLAPPED;

#define STD_OUTPUT_HANDLE (DWORD)-11

__declspec(dllimport) HANDLE __stdcall GetStdHandle(DWORD nStdHandle);
__declspec(dllimport) BOOL __stdcall WriteFile(HANDLE hFile, const void* lpBuffer, DWORD nNumberOfBytesToWrite, DWORD* lpNumberOfBytesWritten, LPOVERLAPPED lpOverlapped);

void __nebula_rt_write(uint8_t* buf, int32_t len) {
    HANDLE hStdout = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD written;
    WriteFile(hStdout, buf, (DWORD)len, &written, 0);
}

#else
// Linux raw syscalls (x86_64)
// rax=1 (write), rdi=1 (stdout), rsi=buf, rdx=len
void __nebula_rt_write(uint8_t* buf, int32_t len) {
    long syscall_ret;
    __asm__ volatile (
        "syscall"
        : "=a" (syscall_ret)
        : "a" (1), "D" (1), "S" (buf), "d" (len)
        : "rcx", "r11", "memory"
    );
}
#endif

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
    __nebula_rt_write((uint8_t*)buf, len);
}

// Wrapper for printing string with a newline
void __nebula_rt_println(const uint8_t* buf) {
    __nebula_rt_print(buf);
    __nebula_rt_write((uint8_t*)"\n", 1);
}
