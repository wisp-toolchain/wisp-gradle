package me.alphamode.wisp.bundler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Small api for working with and modifying minecraft's bundler system
 */
public class Bundler implements Closeable {
    private final FileSystem bundlerFs;
    private final List<FileEntry> libraries;
    private final FileEntry serverJar;
    public Bundler(Path bundlerJar) {
        try {
            this.bundlerFs = FileSystems.newFileSystem(bundlerJar);
            this.libraries = new ArrayList<>(readResource("libraries"));
            this.serverJar = readResource("versions").get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FileSystem getBundlerFs() {
        return bundlerFs;
    }

    public FileEntry getServerJar() {
        return serverJar;
    }

    public List<FileEntry> getLibraries() {
        return libraries;
    }

    private List<FileEntry> readResource(String resource) throws Exception {
        resource = resource + ".list";
        Path fullPath = bundlerFs.getPath("/META-INF/" + resource);

        try (InputStream is = bundlerFs.provider().newInputStream(fullPath)) {
            if (is == null) {
                throw new IllegalStateException("Resource " + fullPath + " not found");
            }

            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().map(FileEntry::parseLine).toList();
        }
    }

    private static String computeHash(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream output = Files.newInputStream(file)) {
            output.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
            return byteToHex(digest.digest());
        }
    }

    private static String byteToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);

        for(byte b : bytes) {
            result.append(Character.forDigit(b >> 4 & 15, 16));
            result.append(Character.forDigit(b & 15, 16));
        }

        return result.toString();
    }

    @Override
    public void close() throws IOException {
        this.bundlerFs.close();
    }
}
