package fr.efiacademy.lab.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.startup.Tomcat;

/**
 * Tomcat 7 embarqué partagé par les tests : déploie les 10 WAR de dist/war,
 * chacun sur son context root (scripts/apps.conf). Démarré une seule fois par JVM.
 */
final class LabServer {

    static final class App {
        final String dir;
        final String name;
        final String context;
        final String label;

        App(String dir, String name, String context, String label) {
            this.dir = dir;
            this.name = name;
            this.context = context;
            this.label = label;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Tomcat tomcat;
    private static int port;
    private static File filesDir;

    private LabServer() {
    }

    static File distDir() {
        String d = System.getProperty("efi.dist.dir", "../../dist");
        return new File(d).getAbsoluteFile();
    }

    static List<App> apps() {
        File conf = new File(distDir().getParentFile(), "scripts/apps.conf");
        List<App> list = new ArrayList<App>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(conf), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] p = line.split("\\|");
                list.add(new App(p[0], p[1], p[2], p[3]));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Lecture impossible de " + conf, e);
        }
        return list;
    }

    static File war(App app) {
        return new File(distDir(), "war/" + app.name + ".war");
    }

    static File filesDir() {
        return filesDir;
    }

    static synchronized String base() throws Exception {
        if (tomcat == null) {
            start();
        }
        return "http://127.0.0.1:" + port;
    }

    private static void start() throws Exception {
        File baseDir = Files.createTempDirectory("efi-tomcat7-").toFile();
        filesDir = Files.createTempDirectory("efi-files-").toFile();
        // Configuration de l'application 08 par propriétés JVM (comme sur WebSphere).
        System.setProperty("efi.files.dir", filesDir.getAbsolutePath());
        System.setProperty("efi.files.maxBytes", "65536");
        System.setProperty("efi.chat.adminCode", "code-de-test-integration");

        Tomcat t = new Tomcat();
        t.setBaseDir(baseDir.getAbsolutePath());
        t.setPort(0);
        t.setSilent(true);
        File appBase = new File(baseDir, "webapps");
        if (!appBase.mkdirs()) {
            throw new IOException("Création impossible : " + appBase);
        }
        t.getHost().setAppBase(appBase.getAbsolutePath());
        t.getHost().setAutoDeploy(false);
        t.getConnector().setURIEncoding("UTF-8");
        for (App a : apps()) {
            File w = war(a);
            if (!w.isFile()) {
                throw new IllegalStateException("WAR absent : " + w + " — lancez d'abord scripts/build-all.sh");
            }
            t.addWebapp(a.context, w.getAbsolutePath());
        }
        t.start();
        port = t.getConnector().getLocalPort();
        tomcat = t;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    tomcat.stop();
                    tomcat.destroy();
                } catch (Exception ignored) {
                    // arrêt de la JVM de test
                }
            }
        });
    }
}
