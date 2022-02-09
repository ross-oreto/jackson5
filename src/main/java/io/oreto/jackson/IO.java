package io.oreto.jackson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

class IO {
    static Optional<String> fileText(File file) {
        try {
            return file.exists()
                    ? Optional.of(String.join("\n", Files.readAllLines(file.toPath())))
                    : Optional.empty();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    static Optional<String> resourceText(ClassLoader classLoader, String path, String... resourcePath) {
        return loadResource(classLoader, path, resourcePath)
                .map(is -> new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n")));
    }

    static Optional<String> resourceText(String path, String... resourcePath) {
        return resourceText(IO.class.getClassLoader(), path, resourcePath);
    }

    static Optional<InputStream> loadResource(ClassLoader classLoader, String path, String... resourcePath) {
        InputStream stream = classLoader.getResourceAsStream(Paths.get(path, resourcePath).toString());
        return stream == null ? Optional.empty() : Optional.of(stream);
    }
}