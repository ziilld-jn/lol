package com.example.lucky4.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Saves uploaded member photos to data/uploads/ and serves them at /uploads/... */
@Component
public class PhotoStorage {

    private static final String URL_PREFIX = "/uploads/";

    private final Path directory;

    @Autowired
    public PhotoStorage(@Value("${lucky4.data-dir:data}") String dataDir) {
        this.directory = Paths.get(dataDir).toAbsolutePath().resolve("uploads");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload folder " + directory, e);
        }
    }

    public Path getDirectory() {
        return directory;
    }

    /**
     * Saves the photo and returns its web path, e.g. /uploads/member2-1700000000000.jpg.
     * The file type is checked from the file's contents, not its name, and the
     * saved name is generated here, never taken from the upload.
     */
    public String save(int memberId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a photo first.");
        }

        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        String extension = read == head.length ? detectExtension(head) : null;
        if (extension == null) {
            throw new IllegalArgumentException("Please upload a PNG, JPG, GIF, or WebP image.");
        }

        Files.createDirectories(directory);
        String name = "member" + memberId + "-" + System.currentTimeMillis() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
        return URL_PREFIX + name;
    }

    /** Deletes an old uploaded photo. Built-in images (/images/...) are left alone. */
    public void deleteQuietly(String imagePath) {
        if (imagePath == null || !imagePath.startsWith(URL_PREFIX)) {
            return;
        }
        String name = imagePath.substring(URL_PREFIX.length());
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve(name));
        } catch (IOException ignored) {
            // Not important if an old file can't be removed.
        }
    }

    private static String detectExtension(byte[] h) {
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return "png";
        }
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return "gif";
        }
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}