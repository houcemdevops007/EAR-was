package fr.efiacademy.lab.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.jasper.JspC;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Précompile TOUTES les JSP de chaque WAR avec Jasper (JSP 2.2) en ciblant Java 6,
 * comme le fera WebSphere au premier accès. Détecte les erreurs de syntaxe JSP ou Java
 * que la compilation Maven ne voit pas (les JSP ne sont compilées que dans le conteneur).
 */
@RunWith(Parameterized.class)
public class JspCompilationIT {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> l = new ArrayList<Object[]>();
        for (LabServer.App a : LabServer.apps()) {
            l.add(new Object[] {a});
        }
        return l;
    }

    private final LabServer.App app;

    public JspCompilationIT(LabServer.App app) {
        this.app = app;
    }

    @Test
    public void toutesLesJspCompilentEnJava6() throws Exception {
        File root = Files.createTempDirectory("efi-jspc-" + app.name + "-").toFile();
        File webapp = new File(root, "webapp");
        File out = new File(root, "out");
        unzip(LabServer.war(app), webapp);
        List<File> jsps = new ArrayList<File>();
        collect(webapp, ".jsp", jsps);
        assertTrue(app.name + " : aucune JSP trouvée", jsps.size() > 0);

        JspC jspc = new JspC();
        jspc.setUriroot(webapp.getAbsolutePath());
        jspc.setOutputDir(out.getAbsolutePath());
        jspc.setClassPath(containerClassPath());
        jspc.setCompile(true);
        jspc.setCompilerSourceVM("1.6");
        jspc.setCompilerTargetVM("1.6");
        jspc.setJavaEncoding("UTF-8");
        jspc.setFailOnError(true);
        jspc.setValidateXml(false);
        jspc.execute(); // lève JasperException à la première JSP invalide

        List<File> classes = new ArrayList<File>();
        collect(out, "_jsp.class", classes);
        assertEquals(app.name + " : chaque JSP doit produire une classe", jsps.size(), classes.size());
        for (File c : classes) {
            assertEquals(app.name + " : " + c.getName() + " doit être en bytecode Java 6", 50, major(c));
        }
        System.out.println("[JSPC] " + app.name + " : " + jsps.size() + " JSP compilées (Java 6)");
    }

    /** API Servlet/JSP/EL du conteneur, exactement celles fournies par Tomcat 7 (Java EE 6). */
    private static String containerClassPath() {
        Set<String> paths = new LinkedHashSet<String>();
        for (Class<?> c : new Class<?>[] {javax.servlet.http.HttpServlet.class, javax.servlet.jsp.JspPage.class,
                javax.el.ELContext.class, org.apache.jasper.runtime.HttpJspBase.class}) {
            paths.add(new File(c.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath());
        }
        StringBuilder sb = new StringBuilder();
        for (String p : paths) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(p);
        }
        return sb.toString();
    }

    static int major(File classFile) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(classFile))) {
            in.readInt();
            in.readUnsignedShort();
            return in.readUnsignedShort();
        }
    }

    private static void collect(File dir, String suffix, List<File> into) {
        File[] list = dir.listFiles();
        if (list == null) {
            return;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                collect(f, suffix, into);
            } else if (f.getName().endsWith(suffix)) {
                into.add(f);
            }
        }
    }

    static void unzip(File war, File dest) throws IOException {
        try (JarFile jar = new JarFile(war)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                File f = new File(dest, e.getName());
                if (!f.getCanonicalPath().startsWith(dest.getCanonicalPath())) {
                    throw new IOException("Entrée suspecte : " + e.getName());
                }
                if (e.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                f.getParentFile().mkdirs();
                try (InputStream in = jar.getInputStream(e); OutputStream o = new FileOutputStream(f)) {
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = in.read(b)) != -1) {
                        o.write(b, 0, n);
                    }
                }
            }
        }
    }
}
