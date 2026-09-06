package sb.linux.client.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointsTest {
    @Test
    fun acceptsSiteHostAndItsSubdomains() {
        assertTrue(Endpoints.isInternal("linux.sb"))
        assertTrue(Endpoints.isInternal("www.linux.sb"))
        assertTrue(Endpoints.isInternal("cdn.linux.sb"))
        assertTrue(Endpoints.isInternal("LINUX.SB"))
    }

    @Test
    fun rejectsLookalikeAndMissingHosts() {
        // 同前缀的外部域名是最容易把站内判定写漏的一类，必须拒绝
        assertFalse(Endpoints.isInternal("linux.sb.evil.com"))
        assertFalse(Endpoints.isInternal("notlinux.sb"))
        assertFalse(Endpoints.isInternal("example.com"))
        assertFalse(Endpoints.isInternal(null))
        assertFalse(Endpoints.isInternal(""))
    }
}
