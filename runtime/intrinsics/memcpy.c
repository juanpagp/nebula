#include <stddef.h>

void* memcpy(void* dest, const void* src, size_t n) {
    char* d = (char*)dest;
    const char* s = (const char*)src;
    while (n--) {
        *d++ = *s++;
    }
    return dest;
}

void* memmove(void* dest, const void* src, size_t n) {
    char* d = (char*)dest;
    const char* s = (const char*)src;
    if (d < s) {
        while (n--) {
            *d++ = *s++;
        }
    } else {
        char* d1 = d + n;
        const char* s1 = s + n;
        while (n--) {
            *--d1 = *--s1;
        }
    }
    return dest;
}
