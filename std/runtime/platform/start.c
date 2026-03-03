extern int main();
extern void sys_exit(int code);

extern int main();
extern void sys_exit(int code);

/*
 * _start is the ELF entry point — jumped to by the kernel, NOT called.
 * At entry, RSP is 16-byte aligned (per the System V x86-64 ABI).
 * We must NOT subtract anything before calling main(); the kernel-provided
 * RSP is already 16-byte aligned, so the CALL instruction will push the
 * return address, leaving RSP at 16n-8 inside main(), which satisfies the
 * ABI invariant "at function entry RSP ≡ 8 (mod 16)".
 * Using naked avoids any compiler-generated prolog that would misalign RSP.
 */
__attribute__((naked)) void _start()
{
    __asm__ volatile (
        "xor %rbp, %rbp\n\t"
        "call main\n\t"
        "mov %eax, %edi\n\t"
        "call sys_exit\n\t"
        "ud2\n\t"
    );
}
