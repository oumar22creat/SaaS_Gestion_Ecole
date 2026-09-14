package com.schoolsaas.document;

import com.schoolsaas.common.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stockage local (dev/MVP) — voir la javadoc de {@link StorageGateway}. */
@Component
public class LocalDiskStorageGateway implements StorageGateway {

    private final Path root;

    public LocalDiskStorageGateway(StorageProperties properties) {
        this.root = Path.of(properties.localPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le répertoire de stockage local : " + this.root, e);
        }
    }

    @Override
    public String store(byte[] content, String fileName) {
        String storageKey = UUID.randomUUID() + "-" + sanitize(fileName);
        try {
            Files.write(root.resolve(storageKey), content);
        } catch (IOException e) {
            throw ApiException.unprocessable("STORAGE_WRITE_FAILED", "Impossible d'enregistrer le fichier");
        }
        return storageKey;
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return Files.readAllBytes(resolveWithinRoot(storageKey));
        } catch (IOException e) {
            throw ApiException.notFound("DOCUMENT_FILE_NOT_FOUND", "Fichier introuvable");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveWithinRoot(storageKey));
        } catch (IOException e) {
            throw ApiException.unprocessable("STORAGE_DELETE_FAILED", "Impossible de supprimer le fichier");
        }
    }

    /** Empêche un storageKey malveillant (ex. "../../etc/passwd") de sortir du répertoire de stockage. */
    private Path resolveWithinRoot(String storageKey) {
        Path resolved = root.resolve(storageKey).toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw ApiException.badRequest("INVALID_STORAGE_KEY", "Clé de stockage invalide", java.util.List.of());
        }
        return resolved;
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
