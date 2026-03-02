#include <stddef.h>
#include <stdint.h>

// Prototypes for internal use
extern void* sys_mmap(void* addr, size_t length, int prot, int flags, int fd, long offset);

#define PROT_READ  0x1
#define PROT_WRITE 0x2
#define MAP_PRIVATE 0x02
#define MAP_ANONYMOUS 0x20

static uint8_t* heap_start = NULL;
static uint8_t* heap_ptr = NULL;
static size_t heap_size = 0;

void* neb_alloc(size_t size) {
    // 8-byte alignment
    size = (size + 7) & ~7;

    if (heap_start == NULL) {
        // Initial allocation: 1MB for now
        heap_size = 1024 * 1024;
        heap_start = (uint8_t*)sys_mmap(NULL, heap_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if ((intptr_t)heap_start <= 0) {
            return NULL;
        }
        heap_ptr = heap_start;
    }

    if (heap_ptr + size > heap_start + heap_size) {
        // Out of memory (bump allocator doesn't resize yet)
        return NULL;
    }

    void* ptr = heap_ptr;
    heap_ptr += size;
    return ptr;
}

void neb_free(void* ptr) {
    // Bump allocator: free is a no-op
}
