#include <stdint.h>
#include <stddef.h>

// =============================================================================
// External dependencies
// =============================================================================

extern void* neb_alloc(size_t size);
extern void* memcpy(void* dest, const void* src, size_t n);
extern void* memset(void* s, int c, size_t n);

// =============================================================================
// Platform syscall prototypes (implemented in linux_net_syscalls.c)
// =============================================================================

extern long sys_net_socket(int domain, int type, int protocol);
extern long sys_net_bind(int fd, const void* addr, unsigned int addrlen);
extern long sys_net_listen(int fd, int backlog);
extern long sys_net_accept(int fd, void* addr, unsigned int* addrlen);
extern long sys_net_accept4(int fd, void* addr, unsigned int* addrlen, int flags);
extern long sys_net_connect(int fd, const void* addr, unsigned int addrlen);
extern long sys_net_sendto(int fd, const void* buf, long count,
                           int flags, const void* addr, unsigned int addrlen);
extern long sys_net_recvfrom(int fd, void* buf, long count,
                             int flags, void* addr, unsigned int* addrlen);
extern long sys_net_setsockopt(int fd, int level, int optname,
                               const void* optval, unsigned int optlen);
extern long sys_net_getsockopt(int fd, int level, int optname,
                               void* optval, unsigned int* optlen);
extern long sys_net_shutdown(int fd, int how);
extern long sys_net_close(int fd);
extern long sys_net_fcntl(int fd, int cmd, long arg);
extern long sys_net_poll(void* fds, unsigned int nfds, int timeout);
extern long sys_net_epoll_create1(int flags);
extern long sys_net_epoll_ctl(int epfd, int op, int fd, void* event);
extern long sys_net_epoll_wait(int epfd, void* events, int maxevents, int timeout);

// =============================================================================
// Struct definitions — must mirror the Nebula struct layouts exactly
// =============================================================================

/*
 * Mirrors Nebula's SockAddrIn (16 bytes).
 * Matches the on-wire layout of Linux sockaddr_in:
 *   offset 0: sin_family  (int16)
 *   offset 2: sin_port    (uint16, network byte order)
 *   offset 4: sin_addr    (uint32, network byte order)
 *   offset 8: sin_zero[8] (padding)
 */
typedef struct
{
    int16_t  family;
    uint16_t port;
    uint32_t addr;
    uint64_t _pad;
} NebulaSockAddrIn;

/*
 * Mirrors Nebula's PollFd (8 bytes). Matches Linux pollfd exactly.
 */
typedef struct
{
    int32_t fd;
    int16_t events;
    int16_t revents;
} NebulaPollFd;

/*
 * Nebula-visible EpollEvent (16 bytes, naturally aligned).
 * Linux epoll_event is 12 bytes packed; we convert in neb_net_epoll_wait/add/mod.
 *   offset 0: events (uint32)
 *   offset 4: _pad   (uint32, explicit alignment gap)
 *   offset 8: data   (uint64, fd or user tag)
 */
typedef struct
{
    uint32_t events;
    uint32_t _pad;
    uint64_t data;
} NebulaEpollEvent;

/*
 * Linux's actual packed epoll_event (12 bytes).  Used only internally when
 * calling the kernel so that the buffer matches kernel expectations.
 */
typedef struct __attribute__((packed))
{
    uint32_t events;
    uint64_t data;
} LinuxEpollEvent;

/*
 * Nebula str type — two-register SysV ABI: pointer + length.
 * Must match NebulaStr in runtime.c exactly.
 */
typedef struct
{
    const uint8_t* ptr;
    int64_t        len;
} NebulaStr;

// =============================================================================
// Constants (mirrored in std/net.neb)
// =============================================================================

#define AF_INET      2
#define SOCK_STREAM  1
#define SOCK_DGRAM   2
#define SOL_SOCKET   1
#define IPPROTO_TCP  6
#define SO_REUSEADDR 2
#define SO_REUSEPORT 15
#define SO_KEEPALIVE 9
#define SO_ERROR     4
#define TCP_NODELAY  1
#define F_GETFL      3
#define F_SETFL      4
#define O_NONBLOCK   2048

// Maximum event-batch size used as an internal stack buffer in epoll_wait.
#define NEB_EPOLL_BATCH 256

// =============================================================================
// Byte-order utilities  (x86-64 is always little-endian)
// =============================================================================

uint16_t neb_htons(uint16_t x)
{
    return (uint16_t)((x << 8) | (x >> 8));
}

uint32_t neb_htonl(uint32_t x)
{
    return ((x & 0x000000FFu) << 24)
         | ((x & 0x0000FF00u) <<  8)
         | ((x & 0x00FF0000u) >>  8)
         | ((x & 0xFF000000u) >> 24);
}

uint16_t neb_ntohs(uint16_t x)
{
    return neb_htons(x);
}

uint32_t neb_ntohl(uint32_t x)
{
    return neb_htonl(x);
}

/*
 * Parse a dotted-decimal IPv4 string (e.g. "192.168.1.1") and return the
 * address in network byte order, ready to store directly in SockAddrIn.addr.
 * Returns 0 on invalid input (callers treat 0.0.0.0 as INADDR_ANY, which is
 * an acceptable fallback for malformed addresses).
 */
uint32_t neb_inet_aton(NebulaStr ip_str)
{
    const uint8_t* s   = ip_str.ptr;
    int64_t        len = ip_str.len;

    uint32_t result = 0;
    int      octet  = 0;
    int      part   = 0;  /* counts dots seen (0-2 for first three octets) */

    for (int64_t i = 0; i < len; i++)
    {
        uint8_t c = s[i];

        if (c == '.')
        {
            if (part >= 3 || octet > 255)
                return 0;
            result = (result << 8) | (uint32_t)octet;
            octet  = 0;
            part++;
        }
        else if (c >= '0' && c <= '9')
        {
            octet = octet * 10 + (c - '0');
            if (octet > 255)
                return 0;
        }
        else
        {
            return 0;
        }
    }

    /* Must have seen exactly 3 dots and the final octet must be valid. */
    if (part != 3 || octet > 255)
        return 0;

    result = (result << 8) | (uint32_t)octet;

    /*
     * result is now in big-endian (network) order as a uint32_t value
     * (e.g. 192.168.1.1 → 0xC0A80101).
     * Byte-swap via htonl so that its in-memory representation is the
     * correct network byte order on little-endian x86-64.
     */
    return neb_htonl(result);
}

// =============================================================================
// Internal helper: fill a NebulaSockAddrIn in-place
// =============================================================================

static void fill_addr(NebulaSockAddrIn* a, uint32_t ip_net, uint16_t port_net)
{
    a->family = (int16_t)AF_INET;
    a->port   = port_net;
    a->addr   = ip_net;
    a->_pad   = 0;
}

// =============================================================================
// Layer 1 — thin C wrappers over platform syscalls
// =============================================================================

int32_t neb_net_socket(int32_t domain, int32_t type, int32_t protocol)
{
    return (int32_t)sys_net_socket(domain, type, protocol);
}

int32_t neb_net_bind(int32_t fd, NebulaSockAddrIn* addr, int32_t addrlen)
{
    return (int32_t)sys_net_bind(fd, (const void*)addr, (unsigned int)addrlen);
}

int32_t neb_net_listen(int32_t fd, int32_t backlog)
{
    return (int32_t)sys_net_listen(fd, backlog);
}

int32_t neb_net_accept_full(int32_t fd, NebulaSockAddrIn* addr, int32_t* addrlen)
{
    unsigned int ulen = addr ? (unsigned int)*addrlen : 0;
    long         ret  = sys_net_accept(fd, (void*)addr, addr ? &ulen : (unsigned int*)0);
    if (addr)
        *addrlen = (int32_t)ulen;
    return (int32_t)ret;
}

int32_t neb_net_accept_fd(int32_t fd)
{
    return (int32_t)sys_net_accept(fd, (void*)0, (unsigned int*)0);
}

int32_t neb_net_connect(int32_t fd, NebulaSockAddrIn* addr, int32_t addrlen)
{
    return (int32_t)sys_net_connect(fd, (const void*)addr, (unsigned int)addrlen);
}

int64_t neb_net_send(int32_t fd, const void* buf, int64_t len, int32_t flags)
{
    return (int64_t)sys_net_sendto(fd, buf, (long)len, flags, (void*)0, 0);
}

int64_t neb_net_recv(int32_t fd, void* buf, int64_t len, int32_t flags)
{
    return (int64_t)sys_net_recvfrom(fd, buf, (long)len, flags, (void*)0, (unsigned int*)0);
}

int64_t neb_net_sendto(int32_t fd, const void* buf, int64_t len, int32_t flags,
                       NebulaSockAddrIn* addr, int32_t addrlen)
{
    return (int64_t)sys_net_sendto(fd, buf, (long)len, flags,
                                   (const void*)addr, (unsigned int)addrlen);
}

int64_t neb_net_recvfrom(int32_t fd, void* buf, int64_t len, int32_t flags,
                         NebulaSockAddrIn* addr, int32_t* addrlen)
{
    unsigned int ulen = addr ? (unsigned int)*addrlen : 0;
    long         ret  = sys_net_recvfrom(fd, buf, (long)len, flags,
                                         (void*)addr, addr ? &ulen : (unsigned int*)0);
    if (addr)
        *addrlen = (int32_t)ulen;
    return (int64_t)ret;
}

int32_t neb_net_setsockopt(int32_t fd, int32_t level, int32_t optname,
                           const void* optval, int32_t optlen)
{
    return (int32_t)sys_net_setsockopt(fd, level, optname, optval, (unsigned int)optlen);
}

int32_t neb_net_getsockopt(int32_t fd, int32_t level, int32_t optname,
                           void* optval, int32_t* optlen)
{
    unsigned int ulen = (unsigned int)*optlen;
    int32_t      ret  = (int32_t)sys_net_getsockopt(fd, level, optname, optval, &ulen);
    *optlen = (int32_t)ulen;
    return ret;
}

int32_t neb_net_shutdown(int32_t fd, int32_t how)
{
    return (int32_t)sys_net_shutdown(fd, how);
}

int32_t neb_net_close(int32_t fd)
{
    return (int32_t)sys_net_close(fd);
}

// =============================================================================
// Layer 2 — convenience helpers
// =============================================================================

/*
 * Create a bound-and-listening TCP server socket.
 *
 * ip_net   – IPv4 address in network byte order (0 = INADDR_ANY).
 * port_host – Port in host byte order (e.g. 8080).
 * backlog  – Listen queue depth.
 *
 * Returns the server fd on success, or a negative error code.
 */
int32_t neb_net_tcp_server(uint32_t ip_net, uint16_t port_host, int32_t backlog)
{
    int32_t fd = neb_net_socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0)
        return fd;

    /* Enable SO_REUSEADDR so the port is immediately reusable after restart. */
    int32_t one = 1;
    neb_net_setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));

    NebulaSockAddrIn addr;
    fill_addr(&addr, ip_net, neb_htons(port_host));

    if (neb_net_bind(fd, &addr, (int32_t)sizeof(NebulaSockAddrIn)) < 0)
    {
        sys_net_close(fd);
        return -1;
    }

    if (neb_net_listen(fd, backlog) < 0)
    {
        sys_net_close(fd);
        return -1;
    }

    return fd;
}

/*
 * Open a TCP connection to ip_net:port_host.
 *
 * ip_net   – IPv4 address in network byte order.
 * port_host – Port in host byte order.
 *
 * Returns the connected socket fd on success, or a negative error code.
 */
int32_t neb_net_tcp_connect(uint32_t ip_net, uint16_t port_host)
{
    int32_t fd = neb_net_socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0)
        return fd;

    NebulaSockAddrIn addr;
    fill_addr(&addr, ip_net, neb_htons(port_host));

    if (neb_net_connect(fd, &addr, (int32_t)sizeof(NebulaSockAddrIn)) < 0)
    {
        sys_net_close(fd);
        return -1;
    }

    return fd;
}

/* Forward declaration — defined below */
int32_t neb_net_set_nonblocking(int32_t fd);

/*
 * Open a TCP connection with a timeout (in seconds).
 * Uses non-blocking connect + poll to enforce the deadline.
 * Returns the connected fd on success (restored to blocking mode),
 * or a negative error code on failure/timeout.
 */
int32_t neb_net_tcp_connect_timeout(uint32_t ip_net, uint16_t port_host, int32_t timeout_secs)
{
    int32_t fd = neb_net_socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0)
        return fd;

    /* Set non-blocking for connect */
    neb_net_set_nonblocking(fd);

    NebulaSockAddrIn addr;
    fill_addr(&addr, ip_net, neb_htons(port_host));

    long rc = sys_net_connect(fd, &addr, (uint32_t)sizeof(NebulaSockAddrIn));
    if (rc < 0)
    {
        /* EINPROGRESS = -115 on Linux — connect is proceeding asynchronously */
        if (rc != -115)
        {
            sys_net_close(fd);
            return -1;
        }
    }
    else
    {
        /* Connected immediately — restore blocking mode and return */
        long flags = sys_net_fcntl(fd, F_GETFL, 0);
        if (flags >= 0)
            sys_net_fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
        return fd;
    }

    /* Poll for writability (connect completion) with timeout */
    NebulaPollFd pfd;
    pfd.fd      = fd;
    pfd.events  = 4; /* POLLOUT */
    pfd.revents = 0;

    long poll_rc = sys_net_poll(&pfd, 1, timeout_secs * 1000);
    if (poll_rc <= 0)
    {
        /* Timeout or error */
        sys_net_close(fd);
        return -1;
    }

    /* Check for connection error */
    if (pfd.revents & (8 | 16))  /* POLLERR | POLLHUP */
    {
        sys_net_close(fd);
        return -1;
    }

    /* Verify the socket is actually connected by checking SO_ERROR */
    int32_t sock_err = 0;
    uint32_t err_len = sizeof(sock_err);
    sys_net_getsockopt(fd, SOL_SOCKET, SO_ERROR, &sock_err, &err_len);
    if (sock_err != 0)
    {
        sys_net_close(fd);
        return -1;
    }

    /* Restore blocking mode */
    long flags = sys_net_fcntl(fd, F_GETFL, 0);
    if (flags >= 0)
        sys_net_fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);

    return fd;
}

/* Create an unbound UDP socket. */
int32_t neb_net_udp_socket(void)
{
    return neb_net_socket(AF_INET, SOCK_DGRAM, 0);
}

/* Set the O_NONBLOCK flag on fd. Returns 0 on success, negative on error. */
int32_t neb_net_set_nonblocking(int32_t fd)
{
    long flags = sys_net_fcntl(fd, F_GETFL, 0);
    if (flags < 0)
        return (int32_t)flags;
    return (int32_t)sys_net_fcntl(fd, F_SETFL, flags | O_NONBLOCK);
}

/* Enable SO_REUSEADDR on fd. */
int32_t neb_net_set_reuseaddr(int32_t fd)
{
    int32_t one = 1;
    return neb_net_setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
}

/* Enable SO_REUSEPORT on fd. */
int32_t neb_net_set_reuseport(int32_t fd)
{
    int32_t one = 1;
    return neb_net_setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, &one, sizeof(one));
}

/* Disable Nagle's algorithm (TCP_NODELAY) on fd. */
int32_t neb_net_set_nodelay(int32_t fd)
{
    int32_t one = 1;
    return neb_net_setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &one, sizeof(one));
}

/* Enable TCP keep-alive probes on fd. */
int32_t neb_net_set_keepalive(int32_t fd)
{
    int32_t one = 1;
    return neb_net_setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &one, sizeof(one));
}

/*
 * Set receive timeout on fd.
 * timeout_secs – timeout in seconds (0 = disable timeout).
 * Returns 0 on success, negative on error.
 *
 * Linux timeval is { long tv_sec; long tv_usec; } = 16 bytes on x86-64.
 */
#define SO_RCVTIMEO 20
#define SO_SNDTIMEO 21

int32_t neb_net_set_recv_timeout(int32_t fd, int32_t timeout_secs)
{
    struct { long tv_sec; long tv_usec; } tv;
    tv.tv_sec  = timeout_secs;
    tv.tv_usec = 0;
    return (int32_t)sys_net_setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO,
                                        &tv, sizeof(tv));
}

int32_t neb_net_set_send_timeout(int32_t fd, int32_t timeout_secs)
{
    struct { long tv_sec; long tv_usec; } tv;
    tv.tv_sec  = timeout_secs;
    tv.tv_usec = 0;
    return (int32_t)sys_net_setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO,
                                        &tv, sizeof(tv));
}

// =============================================================================
// Poll
// =============================================================================

int32_t neb_net_poll(NebulaPollFd* fds, int32_t nfds, int32_t timeout_ms)
{
    return (int32_t)sys_net_poll((void*)fds, (unsigned int)nfds, timeout_ms);
}

// =============================================================================
// Epoll
// =============================================================================

int32_t neb_net_epoll_create(void)
{
    return (int32_t)sys_net_epoll_create1(0);
}

/*
 * Build a packed LinuxEpollEvent on the stack and call epoll_ctl.
 * This avoids exposing the packed struct to Nebula, whose natural alignment
 * would make EpollEvent 16 bytes instead of the kernel's 12.
 */
static int32_t epoll_ctl_packed(int32_t epfd, int32_t op, int32_t fd,
                                 uint32_t events, uint64_t data)
{
    LinuxEpollEvent ev;
    ev.events = events;
    ev.data   = data;
    return (int32_t)sys_net_epoll_ctl(epfd, op, fd, (void*)&ev);
}

int32_t neb_net_epoll_add(int32_t epfd, int32_t fd, uint32_t events, uint64_t data)
{
    return epoll_ctl_packed(epfd, 1 /* EPOLL_CTL_ADD */, fd, events, data);
}

int32_t neb_net_epoll_mod(int32_t epfd, int32_t fd, uint32_t events, uint64_t data)
{
    return epoll_ctl_packed(epfd, 3 /* EPOLL_CTL_MOD */, fd, events, data);
}

int32_t neb_net_epoll_del(int32_t epfd, int32_t fd)
{
    /* epoll_ctl DEL ignores the event argument — pass NULL. */
    return (int32_t)sys_net_epoll_ctl(epfd, 2 /* EPOLL_CTL_DEL */, fd, (void*)0);
}

/*
 * Wait for events on epfd, converting from the kernel's packed 12-byte
 * LinuxEpollEvent to Nebula's naturally-aligned 16-byte NebulaEpollEvent.
 *
 * max is clamped to NEB_EPOLL_BATCH to avoid unbounded stack usage.
 */
int32_t neb_net_epoll_wait(int32_t epfd, NebulaEpollEvent* neb_events,
                            int32_t max, int32_t timeout_ms)
{
    int32_t        batch = (max < NEB_EPOLL_BATCH) ? max : NEB_EPOLL_BATCH;
    LinuxEpollEvent linux_buf[NEB_EPOLL_BATCH];

    int32_t ret = (int32_t)sys_net_epoll_wait(epfd, (void*)linux_buf, batch, timeout_ms);
    if (ret <= 0)
        return ret;

    for (int32_t i = 0; i < ret; i++)
    {
        neb_events[i].events = linux_buf[i].events;
        neb_events[i]._pad   = 0;
        neb_events[i].data   = linux_buf[i].data;
    }

    return ret;
}

// =============================================================================
// DNS resolver  (UDP query to 8.8.8.8:53)
// =============================================================================

/*
 * Encode a domain name in DNS wire format.
 * "example.com" → \x07example\x03com\x00
 * Returns the number of bytes written.
 */
static int dns_encode_name(const uint8_t* name, int64_t name_len, uint8_t* out)
{
    int pos = 0;
    int label_start = pos;
    pos++; /* placeholder for label length */

    for (int64_t i = 0; i < name_len; i++)
    {
        if (name[i] == '.')
        {
            int label_len = pos - label_start - 1;
            out[label_start] = (uint8_t)label_len;
            label_start = pos;
            pos++;
        }
        else
        {
            out[pos++] = name[i];
        }
    }
    /* Final label */
    int label_len = pos - label_start - 1;
    out[label_start] = (uint8_t)label_len;
    out[pos++] = 0; /* root terminator */
    return pos;
}

/*
 * Build a DNS A-record query packet.
 * Returns the total packet length.
 */
static int dns_build_query(const uint8_t* name, int64_t name_len,
                           uint8_t* pkt, uint16_t txid)
{
    /* Header: 12 bytes */
    pkt[0] = (uint8_t)(txid >> 8);
    pkt[1] = (uint8_t)(txid & 0xFF);
    pkt[2] = 0x01; /* flags: RD=1 (recursion desired) */
    pkt[3] = 0x00;
    pkt[4] = 0x00; pkt[5] = 0x01; /* QDCOUNT = 1 */
    pkt[6] = 0x00; pkt[7] = 0x00; /* ANCOUNT */
    pkt[8] = 0x00; pkt[9] = 0x00; /* NSCOUNT */
    pkt[10] = 0x00; pkt[11] = 0x00; /* ARCOUNT */

    /* Question section */
    int qname_len = dns_encode_name(name, name_len, pkt + 12);
    int off = 12 + qname_len;

    /* QTYPE = A (1), QCLASS = IN (1) */
    pkt[off++] = 0x00; pkt[off++] = 0x01;
    pkt[off++] = 0x00; pkt[off++] = 0x01;

    return off;
}

/*
 * Skip over a DNS name in the response (handles compression pointers).
 * Returns the number of bytes consumed.
 */
static int dns_skip_name(const uint8_t* pkt, int off, int pkt_len)
{
    int jumped = 0;
    int consumed = 0;
    while (off < pkt_len)
    {
        uint8_t b = pkt[off];
        if (b == 0)
        {
            if (!jumped) consumed = off + 1;
            else if (consumed == 0) consumed = off;
            break;
        }
        if ((b & 0xC0) == 0xC0)
        {
            /* Compression pointer: 2 bytes */
            if (!jumped)
            {
                consumed = off + 2;
                jumped = 1;
            }
            off = ((b & 0x3F) << 8) | pkt[off + 1];
            continue;
        }
        off += b + 1;
    }
    return consumed ? consumed : off;
}

/*
 * Parse the DNS response and extract the first A record (IPv4 address).
 * Returns the IP in network byte order, or 0 on failure.
 */
static uint32_t dns_parse_response(const uint8_t* pkt, int pkt_len)
{
    if (pkt_len < 12) return 0;

    uint16_t ancount = ((uint16_t)pkt[6] << 8) | pkt[7];
    if (ancount == 0) return 0;

    /* Skip header (12 bytes) */
    int off = 12;

    /* Skip question section: QDCOUNT questions */
    uint16_t qdcount = ((uint16_t)pkt[4] << 8) | pkt[5];
    for (uint16_t q = 0; q < qdcount; q++)
    {
        off = dns_skip_name(pkt, off, pkt_len);
        off += 4; /* QTYPE + QCLASS */
    }

    /* Parse answer records */
    for (uint16_t a = 0; a < ancount && off < pkt_len; a++)
    {
        off = dns_skip_name(pkt, off, pkt_len);
        if (off + 10 > pkt_len) return 0;

        uint16_t rtype    = ((uint16_t)pkt[off] << 8) | pkt[off + 1];
        /* uint16_t rclass = ((uint16_t)pkt[off + 2] << 8) | pkt[off + 3]; */
        /* uint32_t ttl    = ... */
        uint16_t rdlength = ((uint16_t)pkt[off + 8] << 8) | pkt[off + 9];
        off += 10;

        if (rtype == 1 && rdlength == 4 && off + 4 <= pkt_len)
        {
            /* A record: 4 bytes, already in network byte order */
            uint32_t ip;
            memcpy(&ip, pkt + off, 4);
            return ip;
        }

        off += rdlength;
    }

    return 0;
}

/*
 * Resolve a hostname to an IPv4 address via DNS.
 *
 * Sends a UDP DNS query to 8.8.8.8:53 and parses the first A record.
 * Returns the IP in network byte order, or 0 on failure.
 *
 * Falls back to 1.1.1.1 on failure.
 */
uint32_t neb_net_dns_resolve(NebulaStr hostname)
{
    if (hostname.len == 0 || hostname.len > 253) return 0;

    /* Build query packet */
    uint8_t query[512];
    uint16_t txid = 0x1234; /* simple fixed ID */
    int qlen = dns_build_query(hostname.ptr, hostname.len, query, txid);

    /* Create UDP socket */
    int32_t fd = (int32_t)sys_net_socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) return 0;

    /* DNS server: 8.8.8.8 */
    NebulaSockAddrIn dns_addr;
    dns_addr.family = (int16_t)AF_INET;
    dns_addr.port   = neb_htons(53);
    dns_addr.addr   = 0x08080808; /* 8.8.8.8 — symmetric, same in all byte orders */
    dns_addr._pad   = 0;

    /* Send query */
    long sent = sys_net_sendto(fd, query, (long)qlen, 0,
                               (const void*)&dns_addr, sizeof(NebulaSockAddrIn));
    if (sent < 0)
    {
        sys_net_close(fd);
        return 0;
    }

    /* Receive response with a simple timeout: use poll with 3 second timeout */
    NebulaPollFd pfd;
    pfd.fd      = fd;
    pfd.events  = 1; /* POLLIN */
    pfd.revents = 0;
    long pret = sys_net_poll((void*)&pfd, 1, 3000);
    if (pret <= 0)
    {
        sys_net_close(fd);
        return 0;
    }

    uint8_t response[512];
    long rlen = sys_net_recvfrom(fd, response, 512, 0, (void*)0, (unsigned int*)0);
    sys_net_close(fd);

    if (rlen < 12) return 0;

    return dns_parse_response(response, (int)rlen);
}

// =============================================================================
// String-based send/receive helpers
// =============================================================================

/*
 * Send an entire Nebula string over a socket. Retries short writes.
 * Returns total bytes sent, or negative on error.
 */
int64_t neb_net_send_str(int32_t fd, NebulaStr s)
{
    int64_t total = 0;
    while (total < s.len)
    {
        long n = sys_net_sendto(fd, s.ptr + total, s.len - total,
                                0, (void*)0, 0);
        if (n <= 0)
        {
            if (n == 0) break;
            return (int64_t)n; /* error */
        }
        total += n;
    }
    return total;
}

/*
 * Receive data from a socket until the connection is closed (recv returns 0)
 * or an error occurs. Returns the accumulated data as a Nebula str.
 *
 * Uses a growing buffer (doubled on each resize). max_size caps the total.
 * Returns an empty string on error or if nothing was received.
 */
NebulaStr neb_net_recv_all(int32_t fd, int64_t max_size)
{
    int64_t  buf_cap = 4096;
    int64_t  total   = 0;
    uint8_t* buf     = (uint8_t*)neb_alloc((size_t)buf_cap);
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    while (total < max_size)
    {
        int64_t space = buf_cap - total;
        if (space < 1024)
        {
            /* Grow buffer */
            int64_t  new_cap = buf_cap * 2;
            if (new_cap > max_size) new_cap = max_size + 1024;
            uint8_t* new_buf = (uint8_t*)neb_alloc((size_t)new_cap);
            if (!new_buf) break;
            memcpy(new_buf, buf, (size_t)total);
            buf     = new_buf;
            buf_cap = new_cap;
            space   = buf_cap - total;
        }

        long n = sys_net_recvfrom(fd, buf + total, space,
                                   0, (void*)0, (unsigned int*)0);
        if (n <= 0) break;
        total += n;
    }

    if (total == 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    /* Null-terminate for safety */
    if (total < buf_cap)
        buf[total] = '\0';

    return (NebulaStr){ buf, total };
}

/*
 * Receive exactly len bytes, retrying on short reads.
 * Returns the data as a Nebula str. May return shorter on EOF or error.
 */
NebulaStr neb_net_recv_exact(int32_t fd, int64_t len)
{
    if (len <= 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    uint8_t* buf = (uint8_t*)neb_alloc((size_t)(len + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    int64_t total = 0;
    while (total < len)
    {
        long n = sys_net_recvfrom(fd, buf + total, len - total,
                                   0, (void*)0, (unsigned int*)0);
        if (n <= 0) break;
        total += n;
    }

    buf[total] = '\0';
    return (NebulaStr){ buf, total };
}
