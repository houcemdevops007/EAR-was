package fr.efiacademy.lab.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AccessAndHeadersTest {

    @Test
    public void cidrMatchingIpv4AndIpv6() {
        LabAccessFilter f = new LabAccessFilter();
        f.configure("127.0.0.0/8, ::1, 172.16.0.0/12, 192.168.1.0/24, invalide, 10.1.2.3");
        assertTrue(f.isAllowed("127.0.0.1"));
        assertTrue(f.isAllowed("172.16.3.21"));
        assertTrue(f.isAllowed("172.31.255.254"));
        assertFalse(f.isAllowed("172.32.0.1"));
        assertTrue(f.isAllowed("192.168.1.77"));
        assertFalse(f.isAllowed("192.168.2.1"));
        assertTrue(f.isAllowed("10.1.2.3"));
        assertFalse(f.isAllowed("10.1.2.4"));
        assertTrue(f.isAllowed("::1"));
        assertTrue(f.isAllowed("0:0:0:0:0:0:0:1"));
        assertTrue(f.isAllowed("::ffff:172.16.3.20"));
        assertFalse(f.isAllowed("8.8.8.8"));
        assertFalse(f.isAllowed("2001:db8::1"));
    }

    @Test
    public void neverResolvesHostnames() {
        assertNull(LabAccessFilter.Cidr.literal("localhost"));
        assertNull(LabAccessFilter.Cidr.literal("vm-was-efi"));
        assertNull(LabAccessFilter.Cidr.literal("999.1.1.1"));
        LabAccessFilter f = new LabAccessFilter();
        f.configure("127.0.0.0/8");
        assertFalse(f.isAllowed("localhost"));
        assertFalse(f.isAllowed(null));
    }

    @Test
    public void wildcardAllowsEverything() {
        LabAccessFilter f = new LabAccessFilter();
        f.configure("*");
        assertTrue(f.isAllowed("8.8.8.8"));
        f.configure("");
        assertFalse(f.isAllowed("127.0.0.1"));
    }

    @Test
    public void parsesServlet30ContentDisposition() {
        assertEquals("rapport.pdf", FileServlet.fileNameFromDisposition("form-data; name=\"file\"; filename=\"rapport.pdf\""));
        assertEquals("C:\\tmp\\a.txt", FileServlet.fileNameFromDisposition("form-data; name=\"file\"; filename=\"C:\\tmp\\a.txt\""));
        assertEquals("résumé.txt", FileServlet.fileNameFromDisposition(
                "form-data; name=\"file\"; filename=\"r_sum_.txt\"; filename*=UTF-8''r%C3%A9sum%C3%A9.txt"));
        assertNull(FileServlet.fileNameFromDisposition("form-data; name=\"_csrf\""));
        assertNull(FileServlet.fileNameFromDisposition(null));
    }

    @Test
    public void buildsSafeAttachmentHeader() {
        String h = FileServlet.contentDisposition("Cours \"WAS\"; été.pdf");
        assertTrue(h.startsWith("attachment; filename=\"Cours _WAS__ _t_.pdf\""));
        assertTrue(h.endsWith("filename*=UTF-8''Cours%20%22WAS%22%3B%20%C3%A9t%C3%A9.pdf"));
    }
}
