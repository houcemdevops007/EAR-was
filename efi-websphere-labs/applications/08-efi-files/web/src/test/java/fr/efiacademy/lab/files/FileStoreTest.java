package fr.efiacademy.lab.files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileStoreTest {

    private File dir;
    private FileStore store;

    @Before
    public void setUp() throws IOException {
        dir = File.createTempFile("efi-files", "");
        assertTrue(dir.delete());
        store = new FileStore(new File(dir, "store"), 1024, 3, FileStore.parseExtensions("txt, .PNG ,pdf,bad ext"));
    }

    @After
    public void tearDown() {
        delete(dir);
    }

    private static void delete(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                delete(k);
            }
        }
        f.delete();
    }

    private static ByteArrayInputStream bytes(String s) {
        try {
            return new ByteArrayInputStream(s.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void parsesExtensionsWhitelist() {
        assertEquals("[txt, png, pdf]", store.getAllowedExtensions().toString());
    }

    @Test
    public void createsDirectoryAndReportsWritable() {
        FileStore.Status st = store.status();
        assertTrue(st.exists);
        assertTrue(st.writable);
    }

    @Test
    public void storesWithGeneratedNameAndListsFile() throws Exception {
        FileMeta m = store.store("C:\\Users\\candidat\\notes TP.txt", bytes("bonjour"), 1000L);
        assertTrue(FileStore.ID.matcher(m.getId()).matches());
        assertEquals("notes TP.txt", m.getName());
        assertEquals(7, m.getSize());
        assertEquals(64, m.getSha256().length());
        assertTrue(new File(store.getBaseDir(), m.getId() + ".bin").isFile());
        assertFalse("le nom d'origine ne doit jamais être utilisé sur disque",
                new File(store.getBaseDir(), "notes TP.txt").exists());
        List<FileMeta> list = store.list();
        assertEquals(1, list.size());
        assertEquals("notes TP.txt", store.get(m.getId()).getName());
        assertNotNull(store.open(m.getId()));
    }

    @Test
    public void rejectsTraversalAndForbiddenExtensions() {
        expect(415, "../../etc/passwd", "root:x");
        expect(415, "script.jsp", "<% %>");
        expect(415, "shell.sh", "#!/bin/sh");
        expect(415, "archive.txt.exe", "MZ");
        assertEquals("passwd", FileStore.sanitizeName("../../etc/passwd"));
        assertEquals("evil.txt", FileStore.sanitizeName("..\\..\\.evil.txt"));
        assertEquals("ab.txt", FileStore.sanitizeName("a<>\"|?*b.txt"));
    }

    @Test
    public void rejectsOversizedAndEmptyFiles() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 1100; i++) {
            big.append('x');
        }
        expect(413, "gros.txt", big.toString());
        expect(400, "vide.txt", "");
        assertEquals("aucun fichier temporaire ne doit subsister", 0, store.getBaseDir().list().length);
    }

    @Test
    public void checksMagicBytes() throws Exception {
        expect(415, "faux.png", "ceci n'est pas une image");
        expect(415, "faux.pdf", "MZ executable");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        assertEquals("png", store.store("logo.png", new ByteArrayInputStream(png), 1L).getExtension());
        assertEquals("pdf", store.store("doc.pdf", bytes("%PDF-1.4 ..."), 2L).getExtension());
    }

    @Test
    public void enforcesMaxCountAndDeletes() throws Exception {
        FileMeta a = store.store("a.txt", bytes("a"), 1L);
        store.store("b.txt", bytes("b"), 2L);
        store.store("c.txt", bytes("c"), 3L);
        expect(507, "d.txt", "d");
        assertEquals("c.txt", store.list().get(0).getName());
        assertTrue(store.delete(a.getId()));
        assertFalse(store.delete(a.getId()));
        assertNull(store.get(a.getId()));
        assertEquals(2, store.count());
        store.store("d.txt", bytes("d"), 4L);
    }

    @Test
    public void rejectsInvalidIdentifiers() {
        assertNull(store.get("../../etc/passwd"));
        assertNull(store.get("ABCDEF"));
        assertNull(store.get(null));
        try {
            store.open("0123456789abcdef0123456789abcdef");
            fail();
        } catch (FileStoreException e) {
            assertEquals(404, e.getStatus());
        }
    }

    @Test
    public void reportsUnwritableDirectory() throws IOException {
        File file = new File(dir, "plain-file");
        dir.mkdirs();
        assertTrue(file.createNewFile());
        FileStore bad = new FileStore(file, 10, 1, FileStore.parseExtensions("txt"));
        assertFalse(bad.status().writable);
        try {
            bad.store("x.txt", bytes("x"), 1L);
            fail();
        } catch (FileStoreException e) {
            assertEquals(503, e.getStatus());
        }
    }

    @Test
    public void humanSizes() {
        assertEquals("512 o", FileStore.human(512));
        assertEquals("2 Ko", FileStore.human(2048));
        assertEquals("5 Mo", FileStore.human(5242880));
        assertEquals("1,5 Mo", FileStore.human(1572864));
    }

    private void expect(int status, String name, String content) {
        try {
            store.store(name, bytes(content), 1L);
            fail("exception attendue pour " + name);
        } catch (FileStoreException e) {
            assertEquals(name, status, e.getStatus());
        }
    }
}
