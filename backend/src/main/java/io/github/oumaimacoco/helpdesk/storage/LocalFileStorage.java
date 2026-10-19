package io.github.oumaimacoco.helpdesk.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${helpdesk.attachments.directory}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create the attachments directory " + root, exception);
        }
    }

    @Override
    public String store(InputStream content) {
        String key = UUID.randomUUID().toString();
        try {
            Files.copy(content, pathOf(key));
            return key;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot store file " + key, exception);
        }
    }

    @Override
    public InputStream open(String key) {
        try {
            return Files.newInputStream(pathOf(key));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read file " + key, exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(pathOf(key));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot delete file " + key, exception);
        }
    }

    private Path pathOf(String key) {
        UUID.fromString(key);
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}