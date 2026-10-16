package fr.efiacademy.lab.dashboard;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.List;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.ServerInfo;

/**
 * Collecte des métriques <strong>non sensibles</strong> de la JVM via les MXBeans
 * standard de {@code java.lang.management} (disponibles depuis Java 5).
 * <p>
 * Volontairement exclus : variables d'environnement, propriétés système
 * complètes, arguments de la JVM (peuvent contenir des mots de passe),
 * classpath, chemins d'installation et paramètres de sécurité.
 */
public final class MetricsCollector {

    private MetricsCollector() {
    }

    /** Compteurs propres à l'application, fournis par le servlet. */
    public static final class AppCounters {
        public long requests;
        public long errors;
        public int activeSessions;
        public long createdSessions;
        public long appStartedAt;
    }

    public static String toJson(AppCounters app, long now) {
        Runtime rt = Runtime.getRuntime();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean run = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean th = ManagementFactory.getThreadMXBean();
        ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();

        JsonWriter w = new JsonWriter().beginObject();

        w.name("server").beginObject()
                .field("hostname", ServerInfo.hostname())
                .field("serverName", ServerInfo.serverName())
                .field("nodeName", ServerInfo.nodeName())
                .field("cellName", ServerInfo.cellName())
                .field("time", ServerInfo.iso(new Date(now)))
                .field("timeParis", ServerInfo.nowParis("EEEE d MMMM yyyy 'à' HH:mm:ss"))
                .field("timezone", ServerInfo.PARIS)
                .endObject();

        w.name("java").beginObject()
                .field("version", safeProp("java.version"))
                .field("vendor", safeProp("java.vendor"))
                .field("vmName", run.getVmName())
                .field("vmVersion", run.getVmVersion())
                .field("specVersion", safeProp("java.specification.version"))
                .endObject();

        w.name("os").beginObject()
                .field("name", os.getName())
                .field("version", os.getVersion())
                .field("arch", os.getArch())
                .field("processors", rt.availableProcessors())
                .field("loadAverage", round2(os.getSystemLoadAverage()))
                .endObject();

        MemoryUsage heap = mem.getHeapMemoryUsage();
        MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();
        long max = heap.getMax() > 0 ? heap.getMax() : rt.maxMemory();
        w.name("memory").beginObject()
                .field("heapUsed", heap.getUsed())
                .field("heapCommitted", heap.getCommitted())
                .field("heapMax", max)
                .field("heapFree", Math.max(0, max - heap.getUsed()))
                .field("heapUsedPercent", max > 0 ? round2(heap.getUsed() * 100.0 / max) : 0)
                .field("nonHeapUsed", nonHeap.getUsed())
                .field("nonHeapCommitted", nonHeap.getCommitted())
                .endObject();

        long gcCount = 0;
        long gcTime = 0;
        w.name("gc").beginArray();
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            long c = Math.max(0, gc.getCollectionCount());
            long t = Math.max(0, gc.getCollectionTime());
            gcCount += c;
            gcTime += t;
            w.beginObject().field("name", gc.getName()).field("count", c).field("timeMs", t).endObject();
        }
        w.endArray();
        w.field("gcTotalCount", gcCount).field("gcTotalTimeMs", gcTime);

        w.name("threads").beginObject()
                .field("live", th.getThreadCount())
                .field("peak", th.getPeakThreadCount())
                .field("daemon", th.getDaemonThreadCount())
                .endObject();

        w.name("classes").beginObject()
                .field("loaded", cl.getLoadedClassCount())
                .field("totalLoaded", cl.getTotalLoadedClassCount())
                .endObject();

        w.name("jvm").beginObject()
                .field("startTime", ServerInfo.iso(new Date(run.getStartTime())))
                .field("uptimeMs", run.getUptime())
                .field("uptimeText", formatDuration(run.getUptime()))
                .endObject();

        w.name("app").beginObject()
                .field("requests", app.requests)
                .field("errors", app.errors)
                .field("activeSessions", app.activeSessions)
                .field("createdSessions", app.createdSessions)
                .field("startedAt", ServerInfo.iso(new Date(app.appStartedAt)))
                .field("uptimeMs", Math.max(0, now - app.appStartedAt))
                .field("uptimeText", formatDuration(Math.max(0, now - app.appStartedAt)))
                .endObject();

        return w.endObject().toString();
    }

    /** Formate une durée en français : « 2 j 03 h 14 min 05 s ». */
    public static String formatDuration(long ms) {
        long s = Math.max(0, ms / 1000);
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        StringBuilder b = new StringBuilder();
        if (d > 0) {
            b.append(d).append(" j ");
        }
        if (d > 0 || h > 0) {
            b.append(two(h)).append(" h ");
        }
        b.append(two(m)).append(" min ").append(two(sec)).append(" s");
        return b.toString();
    }

    static double round2(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return -1;
        }
        return Math.round(v * 100.0) / 100.0;
    }

    private static String two(long v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }

    /** Lecture d'une propriété système non sensible (liste blanche implicite). */
    private static String safeProp(String key) {
        try {
            return System.getProperty(key, "inconnu");
        } catch (SecurityException e) {
            return "non autorisé";
        }
    }
}
