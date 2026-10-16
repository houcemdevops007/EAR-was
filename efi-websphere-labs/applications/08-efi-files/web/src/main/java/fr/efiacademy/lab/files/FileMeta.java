package fr.efiacademy.lab.files;

import java.io.Serializable;

/** Métadonnées d'un fichier stocké (le nom d'origine n'est jamais utilisé sur disque). */
public final class FileMeta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String extension;
    private final long size;
    private final long uploadedAt;
    private final String sha256;

    public FileMeta(String id, String name, String extension, long size, long uploadedAt, String sha256) {
        this.id = id;
        this.name = name;
        this.extension = extension;
        this.size = size;
        this.uploadedAt = uploadedAt;
        this.sha256 = sha256;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public String getSha256() {
        return sha256;
    }
}
