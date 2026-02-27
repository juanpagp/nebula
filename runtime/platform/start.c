extern int main();
extern void sys_exit(int code);

void _start() {
    int result = main();
    sys_exit(result);
}
