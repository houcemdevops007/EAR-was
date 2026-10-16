package fr.efiacademy.lab.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stockage de fichiers sur disque, conçu pour un usage pédagogique sûr :
 * <ul>
 * <li>répertoire configurable, situé hors de l'EAR et hors des répertoires publics ;</li>
 * <li>nom sur disque généré côté serveur (UUID + « .bin ») : aucune collision, aucune
 * traversée de chemin, aucune extension exécutable ;</li>
 * <li>liste blanche d'extensions + contrôle de signature (« magic bytes ») pour les
 * formats binaires courants ;</li>
 * <li>limite de taille appliquée pendant la copie (pas de confiance dans le client) ;</li>
 * <li>écriture atomique (fichier temporaire puis renommage).</li>
 * </ul>
 * Compatible Java 6 (java.io uniquement).
 */
public class FileStore {

    public static final Pattern ID = Pattern.compile("^[a-f0-9]{32}$");
    private static final String DATA_SUFFIX = ".bin";
    private static final String META_SUFFIX = ".properties";
    private static final int MAX_NAME = 150;

    private final File baseDir;
    private final long maxBytes;
    private final int maxCount;
    private final Set<String> allowed;
    private final Object lock = new Object();

    public FileStore(File baseDir, long maxBytes, int maxCount, Set<String> allowedExtensions) {
        this.baseDir = baseDir.getAbsoluteFile();
        this.maxBytes = maxBytes;
        this.maxCount = maxCount;
        this.allowed = new LinkedHashSet<String>();
        for (String e : allowedExtensions) {
            String x = e.trim().toLowerCase(Locale.ROOT);
            if (x.length() > 0) {
                allowed.add(x);
            }
        }
    }

    /** Analyse « pdf, txt ,PNG » → {pdf, txt, png}. */
    public static Set<String> parseExtensions(String csv) {
        Set<String> s = new LinkedHashSet<String>();
        if (csv != null) {
            for (String p : csv.split(",")) {
                String x = p.trim().toLowerCase(Locale.ROOT);
                if (x.startsWith(".")) {
                    x = x.substring(1);
                }
                if (x.matches("[a-z0-9]{1,10}")) {
                    s.add(x);
                }
            }
        }
        return s;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public Set<String> getAllowedExtensions() {
        return Collections.unmodifiableSet(allowed);
    }

    /** État du répertoire de stockage (création si possible). */
    public Status status() {
        Status s = new Status();
        try {
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                s.message = "Le répertoire de stockage n'existe pas et ne peut pas être créé (droits insuffisants).";
                return s;
            }
            if (!baseDir.isDirectory()) {
                s.message = "Le chemin de stockage existe mais n'est pas un répertoire.";
                return s;
            }
            s.exists = true;
            File probe = new File(baseDir, ".probe-" + UUID.randomUUID().toString());
            try {
                OutputStream o = new FileOutputStream(probe);
                o.close();
                s.writable = true;
            } catch (IOException e) {
                s.message = "Le répertoire existe mais n'est pas accessible en écriture pour l'utilisateur du serveur.";
            } finally {
                if (probe.exists() && !probe.delete()) {
                    probe.deleteOnExit();
                }
            }
        } catch (SecurityException e) {
            s.message = "Accès au répertoire refusé par la politique de sécurité Java.";
        }
        if (s.writable) {
            s.message = "Répertoire de stockage opérationnel.";
        }
        return s;
    }

    /** État simple du stockage. */
    public static final class Status {
        public boolean exists;
        public boolean writable;
        public String message;
    }

    /** Nettoie un nom de fichier fourni par le client (affichage uniquement). */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String n = raw;
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) {
            n = n.substring(slash + 1);
        }
        StringBuilder b = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c >= 0x20 && c != 0x7f && "<>:\"|?*".indexOf(c) < 0) {
                b.append(c);
            }
        }
        n = b.toString().trim();
        while (n.startsWith(".")) {
            n = n.substring(1);
        }
        if (n.length() > MAX_NAME) {
            String ext = extensionOf(n);
            int keep = MAX_NAME - ext.length() - 1;
            n = ext.length() > 0 && keep > 0 ? n.substring(0, keep) + "." + ext : n.substring(0, MAX_NAME);
        }
        return n;
    }

    public static String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Enregistre un fichier.
     * @param originalName nom fourni par le navigateur (nettoyé, jamais utilisé sur disque)
     * @param in flux du contenu (non fermé par cette méthode)
     */
    public FileMeta store(String originalName, InputStream in, long now) throws FileStoreException {
        String name = sanitizeName(originalName);
        if (name.length() == 0) {
            throw new FileStoreException(400, "Nom de fichier vide ou invalide.");
        }
        String ext = extensionOf(name);
        if (!allowed.contains(ext)) {
            throw new FileStoreException(415, "Type de fichier non autorisé (." + (ext.length() == 0 ? "?" : ext)
                    + "). Extensions acceptées : " + join(allowed) + ".");
        }
        Status st = status();
        if (!st.writable) {
            throw new FileStoreException(503, st.message);
        }
        synchronized (lock) {
            if (count() >= maxCount) {
                throw new FileStoreException(507, "Nombre maximal de fichiers atteint (" + maxCount + ").");
            }
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        File tmp = new File(baseDir, ".upload-" + id + ".tmp");
        long size = 0;
        byte[] head = new byte[8];
        int headLen = 0;
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new FileStoreException(500, "SHA-256 indisponible.", e);
        }
        OutputStream out = null;
        boolean ok = false;
        try {
            out = new FileOutputStream(tmp);
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                size += r;
                if (size > maxBytes) {
                    throw new FileStoreException(413, "Fichier trop volumineux : limite de " + human(maxBytes) + ".");
                }
                if (headLen < head.length) {
                    int n = Math.min(head.length - headLen, r);
                    System.arraycopy(buf, 0, head, headLen, n);
                    headLen += n;
                }
                sha.update(buf, 0, r);
                out.write(buf, 0, r);
            }
            out.close();
            out = null;
            if (size == 0) {
                throw new FileStoreException(400, "Le fichier est vide.");
            }
            if (!signatureMatches(ext, head, headLen)) {
                throw new FileStoreException(415, "Le contenu ne correspond pas à l'extension ." + ext + ".");
            }
            File data = dataFile(id);
            if (!tmp.renameTo(data)) {
                throw new FileStoreException(500, "Impossible de finaliser l'enregistrement du fichier.");
            }
            FileMeta meta = new FileMeta(id, name, ext, size, now, hex(sha.digest()));
            writeMeta(meta);
            ok = true;
            return meta;
        } catch (IOException e) {
            throw new FileStoreException(500, "Erreur d'écriture sur le disque du serveur.", e);
        } finally {
            closeQuietly(out);
            if (!ok) {
                deleteQuietly(tmp);
            }
        }
    }

    /** Vérifie la signature binaire des formats courants (les formats texte ne sont pas contrôlés). */
    static boolean signatureMatches(String ext, byte[] h, int len) {
        if ("png".equals(ext)) {
            return starts(h, len, 0x89, 'P', 'N', 'G');
        }
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return starts(h, len, 0xFF, 0xD8, 0xFF);
        }
        if ("gif".equals(ext)) {
            return starts(h, len, 'G', 'I', 'F', '8');
        }
        if ("pdf".equals(ext)) {
            return starts(h, len, '%', 'P', 'D', 'F');
        }
        if ("zip".equals(ext) || "docx".equals(ext) || "xlsx".equals(ext) || "pptx".equals(ext) || "odt".equals(ext)) {
            return starts(h, len, 'P', 'K', 0x03, 0x04) || starts(h, len, 'P', 'K', 0x05, 0x06);
        }
        return true;
    }

    private static boolean starts(byte[] h, int len, int... sig) {
        if (len < sig.length) {
            return false;
        }
        for (int i = 0; i < sig.length; i++) {
            if ((h[i] & 0xFF) != sig[i]) {
                return false;
            }
        }
        return true;
    }

    /** Liste des fichiers (plus récents d'abord). */
    public List<FileMeta> list() {
        List<FileMeta> out = new ArrayList<FileMeta>();
        File[] files = baseDir.listFiles();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            String n = f.getName();
            if (n.endsWith(META_SUFFIX)) {
                String id = n.substring(0, n.length() - META_SUFFIX.length());
                FileMeta m = ID.matcher(id).matches() ? readMeta(id) : null;
                if (m != null && dataFile(id).isFile()) {
                    out.add(m);
                }
            }
        }
        Collections.sort(out, new Comparator<FileMeta>() {
            public int compare(FileMeta a, FileMeta b) {
                return a.getUploadedAt() == b.getUploadedAt() ? a.getName().compareToIgnoreCase(b.getName())
                        : (a.getUploadedAt() > b.getUploadedAt() ? -1 : 1);
            }
        });
        return out;
    }

    public int count() {
        File[] files = baseDir.listFiles();
        int c = 0;
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(DATA_SUFFIX)) {
                    c++;
                }
            }
        }
        return c;
    }

    public long totalBytes() {
        long t = 0;
        for (FileMeta m : list()) {
            t += m.getSize();
        }
        return t;
    }

    /** Métadonnées d'un fichier, ou null si l'identifiant est invalide / inconnu. */
    public FileMeta get(String id) {
        if (id == null || !ID.matcher(id).matches()) {
            return null;
        }
        FileMeta m = readMeta(id);
        return m != null && dataFile(id).isFile() ? m : null;
    }

    /** Fichier de données correspondant, avec contrôle de confinement (défense en profondeur). */
    public File open(String id) throws FileStoreException {
        if (get(id) == null) {
            throw new FileStoreException(404, "Fichier introuvable.");
        }
        File f = dataFile(id);
        try {
            String base = baseDir.getCanonicalPath() + File.separator;
            if (!f.getCanonicalPath().startsWith(base)) {
                throw new FileStoreException(400, "Chemin refusé.");
            }
        } catch (IOException e) {
            throw new FileStoreException(500, "Chemin invalide.", e);
        }
        return f;
    }

    public boolean delete(String id) {
        if (get(id) == null) {
            return false;
        }
        synchronized (lock) {
            boolean a = dataFile(id).delete();
            boolean b = new File(baseDir, id + META_SUFFIX).delete();
            return a || b;
        }
    }

    private File dataFile(String id) {
        return new File(baseDir, id + DATA_SUFFIX);
    }

    private void writeMeta(FileMeta m) throws IOException {
        Properties p = new Properties();
        p.setProperty("name", m.getName());
        p.setProperty("extension", m.getExtension());
        p.setProperty("size", String.valueOf(m.getSize()));
        p.setProperty("uploadedAt", String.valueOf(m.getUploadedAt()));
        p.setProperty("sha256", m.getSha256());
        File tmp = new File(baseDir, ".meta-" + m.getId() + ".tmp");
        Writer w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
        try {
            p.store(w, "EFI File Center - metadonnees");
        } finally {
            w.close();
        }
        if (!tmp.renameTo(new File(baseDir, m.getId() + META_SUFFIX))) {
            deleteQuietly(tmp);
            throw new IOException("renommage des métadonnées impossible");
        }
    }

    private FileMeta readMeta(String id) {
        File f = new File(baseDir, id + META_SUFFIX);
        if (!f.isFile()) {
            return null;
        }
        Properties p = new Properties();
        Reader r = null;
        try {
            r = new InputStreamReader(new FileInputStream(f), "UTF-8");
            p.load(r);
            return new FileMeta(id, p.getProperty("name", id), p.getProperty("extension", ""),
                    Long.parseLong(p.getProperty("size", "0")), Long.parseLong(p.getProperty("uploadedAt", "0")),
                    p.getProperty("sha256", ""));
        } catch (IOException e) {
            return null;
        } catch (NumberFormatException e) {
            return null;
        } finally {
            closeQuietly(r);
        }
    }

    public static String human(long bytes) {
        if (bytes < 1024) {
            return bytes + " o";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " Ko";
        }
        double mo = bytes / (1024.0 * 1024.0);
        return (mo == Math.floor(mo) ? String.valueOf((long) mo) : String.format(Locale.FRANCE, "%.1f", mo)) + " Mo";
    }

    private static String join(Set<String> s) {
        StringBuilder b = new StringBuilder();
        for (String x : s) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(x);
        }
        return b.toString();
    }

    private static String hex(byte[] d) {
        StringBuilder b = new StringBuilder(d.length * 2);
        for (byte x : d) {
            b.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
        }
        return b.toString();
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // fermeture best-effort
            }
        }
    }

    private static void deleteQuietly(File f) {
        if (f != null && f.exists() && !f.delete()) {
            f.deleteOnExit();
        }
    }
}
