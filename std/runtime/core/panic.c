extern long sys_write(int fd, const void* buf, long count);
extern void sys_exit(int code);

void neb_panic(const char* message) {
    // Very basic panic
    const char* prefix = "PANIC: ";
    int len = 0;
    while (message[len]) len++;

    sys_write(2, prefix, 7);
    sys_write(2, message, len);
    sys_write(2, "\n", 1);
    sys_exit(1);
}
