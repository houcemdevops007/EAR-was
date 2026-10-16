package fr.efiacademy.lab.files;

/** Erreur fonctionnelle d'upload / d'accès, avec code HTTP associé. */
public class FileStoreException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int status;

    public FileStoreException(int status, String message) {
        super(message);
        this.status = status;
    }

    public FileStoreException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
