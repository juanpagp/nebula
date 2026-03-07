#include <stdint.h>
#include <stddef.h>

// =============================================================================
// Forward declarations
// =============================================================================

extern void* neb_alloc(size_t size);
extern void* memcpy(void* dest, const void* src, size_t n);

typedef struct
{
    const uint8_t* ptr;
    int64_t        len;
} NebulaStr;

// =============================================================================
// String length
// =============================================================================

int64_t neb_str_len(NebulaStr s)
{
    return s.len;
}

// =============================================================================
// Character access
// =============================================================================

int32_t neb_str_char_at(NebulaStr s, int64_t index)
{
    if (index < 0 || index >= s.len)
        return -1;
    return (int32_t)s.ptr[index];
}

// =============================================================================
// Searching
// =============================================================================

/*
 * Find the first occurrence of character ch in s, starting at position start.
 * Returns the index, or -1 if not found.
 */
int64_t neb_str_index_of_char(NebulaStr s, int32_t ch, int64_t start)
{
    if (start < 0) start = 0;
    uint8_t c = (uint8_t)ch;
    for (int64_t i = start; i < s.len; i++)
    {
        if (s.ptr[i] == c)
            return i;
    }
    return -1;
}

/*
 * Find the first occurrence of needle in haystack, starting at position start.
 * Simple brute-force algorithm. Returns the index, or -1 if not found.
 */
int64_t neb_str_index_of(NebulaStr haystack, NebulaStr needle, int64_t start)
{
    if (start < 0) start = 0;
    if (needle.len == 0) return start;
    if (needle.len > haystack.len) return -1;

    int64_t limit = haystack.len - needle.len;
    for (int64_t i = start; i <= limit; i++)
    {
        int64_t j = 0;
        while (j < needle.len && haystack.ptr[i + j] == needle.ptr[j])
        {
            j++;
        }
        if (j == needle.len)
            return i;
    }
    return -1;
}

/*
 * Find the last occurrence of character ch in s.
 * Returns the index, or -1 if not found.
 */
int64_t neb_str_last_index_of_char(NebulaStr s, int32_t ch)
{
    uint8_t c = (uint8_t)ch;
    for (int64_t i = s.len - 1; i >= 0; i--)
    {
        if (s.ptr[i] == c)
            return i;
    }
    return -1;
}

// =============================================================================
// Substring extraction  (allocates via neb_alloc)
// =============================================================================

/*
 * Extract a substring from position start with length len.
 * Clamps to bounds. Returns an empty string on invalid input.
 */
NebulaStr neb_str_substring(NebulaStr s, int64_t start, int64_t len)
{
    if (start < 0) start = 0;
    if (start >= s.len || len <= 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    if (start + len > s.len)
        len = s.len - start;

    uint8_t* buf = (uint8_t*)neb_alloc((size_t)(len + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    memcpy(buf, s.ptr + start, (size_t)len);
    buf[len] = '\0';
    return (NebulaStr){ buf, len };
}

/*
 * Extract a substring from start (inclusive) to end (exclusive).
 */
NebulaStr neb_str_slice(NebulaStr s, int64_t start, int64_t end)
{
    if (start < 0) start = 0;
    if (end > s.len) end = s.len;
    if (start >= end)
        return (NebulaStr){ (const uint8_t*)"", 0 };
    return neb_str_substring(s, start, end - start);
}

// =============================================================================
// Prefix / suffix / contains
// =============================================================================

int32_t neb_str_starts_with(NebulaStr s, NebulaStr prefix)
{
    if (prefix.len > s.len) return 0;
    for (int64_t i = 0; i < prefix.len; i++)
    {
        if (s.ptr[i] != prefix.ptr[i])
            return 0;
    }
    return 1;
}

int32_t neb_str_ends_with(NebulaStr s, NebulaStr suffix)
{
    if (suffix.len > s.len) return 0;
    int64_t off = s.len - suffix.len;
    for (int64_t i = 0; i < suffix.len; i++)
    {
        if (s.ptr[off + i] != suffix.ptr[i])
            return 0;
    }
    return 1;
}

int32_t neb_str_contains(NebulaStr haystack, NebulaStr needle)
{
    return (neb_str_index_of(haystack, needle, 0) >= 0) ? 1 : 0;
}

// =============================================================================
// Case conversion & comparison
// =============================================================================

static uint8_t to_lower_byte(uint8_t c)
{
    if (c >= 'A' && c <= 'Z')
        return c + ('a' - 'A');
    return c;
}

NebulaStr neb_str_to_lower(NebulaStr s)
{
    if (s.len == 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    uint8_t* buf = (uint8_t*)neb_alloc((size_t)(s.len + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    for (int64_t i = 0; i < s.len; i++)
    {
        buf[i] = to_lower_byte(s.ptr[i]);
    }
    buf[s.len] = '\0';
    return (NebulaStr){ buf, s.len };
}

int32_t neb_str_equals(NebulaStr a, NebulaStr b)
{
    if (a.len != b.len) return 0;
    for (int64_t i = 0; i < a.len; i++)
    {
        if (a.ptr[i] != b.ptr[i])
            return 0;
    }
    return 1;
}

int32_t neb_str_equals_ignore_case(NebulaStr a, NebulaStr b)
{
    if (a.len != b.len) return 0;
    for (int64_t i = 0; i < a.len; i++)
    {
        if (to_lower_byte(a.ptr[i]) != to_lower_byte(b.ptr[i]))
            return 0;
    }
    return 1;
}

// =============================================================================
// Parsing
// =============================================================================

/*
 * Parse a decimal integer from string s.
 * Returns the parsed value. On malformed input, returns 0.
 */
int64_t neb_str_parse_i64(NebulaStr s)
{
    if (s.len == 0) return 0;

    int64_t result = 0;
    int64_t i      = 0;
    int     neg    = 0;

    if (s.ptr[0] == '-')
    {
        neg = 1;
        i   = 1;
    }
    else if (s.ptr[0] == '+')
    {
        i = 1;
    }

    for (; i < s.len; i++)
    {
        uint8_t c = s.ptr[i];
        if (c < '0' || c > '9')
            break;
        result = result * 10 + (c - '0');
    }

    return neg ? -result : result;
}

// =============================================================================
// Concatenation  (allocates via neb_alloc)
// =============================================================================

NebulaStr neb_str_concat2(NebulaStr a, NebulaStr b)
{
    int64_t total = a.len + b.len;
    if (total == 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    uint8_t* buf = (uint8_t*)neb_alloc((size_t)(total + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    memcpy(buf, a.ptr, (size_t)a.len);
    memcpy(buf + a.len, b.ptr, (size_t)b.len);
    buf[total] = '\0';
    return (NebulaStr){ buf, total };
}

NebulaStr neb_str_concat3(NebulaStr a, NebulaStr b, NebulaStr c)
{
    int64_t total = a.len + b.len + c.len;
    if (total == 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    uint8_t* buf = (uint8_t*)neb_alloc((size_t)(total + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    int64_t off = 0;
    memcpy(buf + off, a.ptr, (size_t)a.len); off += a.len;
    memcpy(buf + off, b.ptr, (size_t)b.len); off += b.len;
    memcpy(buf + off, c.ptr, (size_t)c.len);
    buf[total] = '\0';
    return (NebulaStr){ buf, total };
}

// =============================================================================
// Trimming
// =============================================================================

static int is_whitespace(uint8_t c)
{
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}

NebulaStr neb_str_trim(NebulaStr s)
{
    int64_t start = 0;
    while (start < s.len && is_whitespace(s.ptr[start]))
        start++;

    int64_t end = s.len;
    while (end > start && is_whitespace(s.ptr[end - 1]))
        end--;

    return neb_str_substring(s, start, end - start);
}

// =============================================================================
// Conversion from raw bytes to Nebula str (copies into neb_alloc buffer)
// =============================================================================

NebulaStr neb_str_from_bytes(const uint8_t* ptr, int64_t len)
{
    if (!ptr || len <= 0)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    uint8_t* buf = (uint8_t*)neb_alloc((size_t)(len + 1));
    if (!buf)
        return (NebulaStr){ (const uint8_t*)"", 0 };

    memcpy(buf, ptr, (size_t)len);
    buf[len] = '\0';
    return (NebulaStr){ buf, len };
}
