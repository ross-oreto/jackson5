package io.oreto.jackson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class Util {
    /**
     * String utils class
     */
    static class Str {
        /**
         * Is String s null or empty
         * @param s The string to evaluate
         * @return boolean true if null or empty, false otherwise
         */
        static boolean isEmpty(final CharSequence s) {
            return s == null || s.length() == 0;
        }

        /**
         * Is String s blank
         * @param s The string to evaluate
         * @return boolean true if string is blank, meaning only whitespace. False otherwise
         */
        static boolean isBlank(CharSequence s) {
            return isEmpty(s) || s.chars().allMatch(Character::isWhitespace);
        }

        /**
         * Is String s not blank
         * @param s The string to evaluate
         * @return boolean true if string is not blank, meaning something other than whitespace. False otherwise
         */
        static boolean isNotBlank(CharSequence s) {
            return !isBlank(s);
        }

        /**
         * Convert String s to an Integer if possible
         * @param s The String representation of a number
         * @return Optional Integer if s in a number, Optional.empty otherwise
         */
        static Optional<Integer> toInteger(CharSequence s) {
            try {
                return Optional.of(Integer.parseInt(s.toString()));
            } catch (NumberFormatException ignored) { }
            return Optional.empty();
        }
    }

    static class MultiMap<K, V> {
        private final Map<K, List<V>> map = new HashMap<>();

        final void put(K k, V v) {
            if ((v instanceof String && !"".equals(v)) || (!(v instanceof String) && v != null)) {
                if (map.containsKey(k)) map.get(k).add(v);
                else map.put(k, new ArrayList<V>() {{
                    add(v);
                }});
            }
        }

        /**
         * Sort each list in the map
         * @param c The comparator used by sort.
         * @return A self referencing MultiMap to support a fluent api.
         */
        MultiMap<K, V> sort(Comparator<? super V> c) {
            map.forEach(((k, vs) -> vs.sort(c)));
            return this;
        }

        Map<K, List<V>> asMap() { return map; }
    }

    static class MultiString<K> extends MultiMap<K, String> {
    }

    static class IO {
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
}
