package me.alphamode.wisp.bundler;

public record FileEntry(String hash, String id, String path) {
    public static FileEntry parseLine(String line) {
        String[] fields = line.split("\t");
        if (fields.length != 3) {
            throw new IllegalStateException("Malformed library entry: " + line);
        } else {
            return new FileEntry(fields[0], fields[1], fields[2]);
        }
    }

    @Override
    public String toString() {
        return hash + "\t" + id + "\t" + path;
    }
}