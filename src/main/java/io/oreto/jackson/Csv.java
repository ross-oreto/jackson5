package io.oreto.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Csv<T> {

    private static List<Map<String, ?>> from(MappingIterator<Map<String, ?>> mappingIterator) {
        try {
            return mappingIterator.readAll();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert CSV text to a List of Maps, in other words row objects
     * @param csv The csv String
     * @return List<Map<String, ?>> rows
     */
    public static List<Map<String, ?>> from(String csv) {
        MappingIterator<Map<String, ?>> mappingIterator = null;
        try {
            //s = s.replaceAll("[ \t]", "");
            mappingIterator = new CsvMapper()
                    .enable(CsvParser.Feature.ALLOW_COMMENTS)
                    .enable(CsvParser.Feature.TRIM_SPACES).reader()
                    .forType(Map.class)
                    .with(CsvSchema.emptySchema().withHeader())
                    .readValues(csv);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mappingIterator == null ? null : from(mappingIterator);
    }

    /**
     * Convert CSV File to a List of Maps, in other words row objects
     * @param csv The csv File
     * @return List<Map<String, ?>> rows
     */
    public static List<Map<String, ?>> from(File csv) {
        return from(IO.fileText(csv).orElse(""));
    }

    private static boolean selected(String name, Options options) {
        if (options.include == null && options.exclude == null)
            return true;
        else if (Objects.nonNull(options.include)) {
            if (Arrays.binarySearch(options.include, name) >= 0)
                return true;
            else if (name.contains(".")
                    && Arrays.binarySearch(options.include, name.substring(0, name.lastIndexOf('.'))) >= 0)
                return true;
        }
        return (Objects.nonNull(options.exclude) && Arrays.binarySearch(options.exclude, name) < 0);
    }

    @SuppressWarnings("unchecked")
    private static void _flatten(String k
            , Object v
            , CsvSchema.Builder csvSchemaBuilder
            , Stack<String> path
            , Map<String, Object> flat
            , Options options) {
        if (v instanceof Map) {
            path.push(k);
            if (selected(String.join(".", path), options))
                _flatten((Map<String, Object>) v, csvSchemaBuilder, path, flat, options);
            path.pop();
        } else {
            path.push(k);
            String name = String.join(".", path);
            String key = csvSchemaBuilder.hasColumn(k) ? name : k;
            path.pop();
            if (selected(name, options)) {
                csvSchemaBuilder.addColumn(key);
                flat.put(key, v);
            }
        }
    }

    private static void _flatten(Map<String, Object> o
            , CsvSchema.Builder csvSchemaBuilder
            , Stack<String> path
            , Map<String, Object> flat
            , Options options) {
        o.forEach((k, v) -> _flatten(k, v, csvSchemaBuilder, path, flat, options));
    }

    private static Map<String, Object> flatten(Map<String, Object> o
            , CsvSchema.Builder csvSchemaBuilder
            , Options options) {
        Map<String, Object> flat = new LinkedHashMap<>();
        _flatten(o, csvSchemaBuilder, new Stack<>(), flat, options);
        return flat;
    }

    private static List<Map<String, Object>> flattenAll(List<Map<String, Object>> list
            , CsvSchema.Builder csvSchemaBuilder
            , Options options) {
       return list.stream().map(it -> flatten(it, csvSchemaBuilder, options)).collect(Collectors.toList());
    }

    /**
     * Convert row map objects into csv String
     * @param o The List of Maps
     * @param options Options representing how the csv is rendered
     * @return The csv String
     * @throws IOException If there is an issue writing CSV
     */
    public static String asCsv(List<Map<String, Object>> o, Options options)
            throws IOException {
        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        if (o.size() == 0)
            options.withoutHeader();
        else
            o = flattenAll(o, csvSchemaBuilder, options);

        CsvSchema csvSchema = csvSchemaBuilder.build()
                .sortedBy(options.order)
                .withUseHeader(options.header);

        if (options.withoutQuote)
            csvSchema = csvSchema.withoutQuoteChar();

        if (Objects.nonNull(options.asc)) {
                csvSchema = csvSchema.sortedBy(options.asc ? Comparator.naturalOrder() : Comparator.reverseOrder());
        }
        return new CsvMapper().writerFor(JsonNode.class)
                .with(csvSchema)
                .writeValueAsString(Jackson5.asJson(o));
    }

    /**
     * Convert row map objects into csv String
     * @param o The List of Maps
     * @return The csv String
     * @throws IOException If there is an issue writing CSV
     */
    public static String asCsv(List<Map<String, Object>> o) throws IOException {
        return asCsv(o, Options.header());
    }

    /**
     * Convert row objects into csv String
     * @param o The List of object
     * @param options Options representing how the csv is rendered
     * @return The csv String
     * @throws IOException If there is an issue writing CSV
     */
    public static String toCsv(Iterable<?> o, Options options) throws IOException {
        Iterator<?> iterator = o.iterator();
        if (iterator.hasNext()) {
            if (Iterable.class.isAssignableFrom(iterator.next().getClass())) {
                return new CsvMapper().writerFor(JsonNode.class)
                        .with(CsvSchema.builder().build().withoutHeader())
                        .writeValueAsString(Jackson5.asJson(o));
            }
        }
        return asCsv(StreamSupport.stream(o.spliterator(), false)
                .map(Jackson5::asMap)
                .collect(Collectors.toList()), options);
    }

    /**
     * Convert row objects into csv String
     * @param o The List of object
     * @return The csv String
     * @throws IOException If there is an issue writing CSV
     */
    public static String toCsv(Iterable<?> o) throws IOException {
       return toCsv(o, Options.header());
    }

    /**
     * Create Csv object from data list
     * @param data An iterable data list representing the CSV rows
     * @param options Options representing how the csv is rendered
     * @return The Csv object
     */
    public static <T> Csv<T> of(Iterable<T> data, Options options) {
       return new Csv<>(data, options);
    }

    /**
     * Create Csv object from data list
     * @param data An iterable data list representing the CSV rows
     * @return The Csv object
     */
    public static <T> Csv<T> of(Iterable<T> data) {
        return new Csv<>(data);
    }

    private final Iterable<T> data;
    private final Options options;

    private Csv(Iterable<T> data, Options options) {
        this.data = data;
        this.options = options;
    }
    private Csv(Iterable<T> data) {
        this.data = data;
        this.options = Options.header();
    }

    /**
     * Write CSV String
     * @return CSV String
     * @throws IOException If there is an issue writing CSV
     */
    public String writeString() throws IOException {
        List<Map<String, Object>> o = StreamSupport.stream(data.spliterator(), false)
                .map(Jackson5::asMap).collect(Collectors.toList());

        return Csv.asCsv(o, options);
    }

    /**
     * Write CSV String to a file
     * @throws IOException If there is an issue writing CSV
     */
    public void write(Path path) throws IOException {
        Files.write(path, writeString().getBytes(StandardCharsets.UTF_8));
    }
    /**
     * Write CSV String to a file
     * @throws IOException If there is an issue writing CSV
     */
    public void write(File file) throws IOException {
        write(file.toPath());
    }
    /**
     * Write CSV String to a file
     * @throws IOException If there is an issue writing CSV
     */
    public void write(String path, String... more) throws IOException {
        write(Paths.get(path, more));
    }
    /**
     * Write CSV String to specified PrintWriter
     * @throws IOException If there is an issue writing CSV
     */
    public void write(PrintWriter printWriter) throws IOException {
        printWriter.write(writeString());
    }

    /**
     * Options describing how the csv is formed/structured.
     */
    public static class Options {
        public static Options header() {
            return new Options();
        }
        public static Options noHeader() {
            return new Options().withoutHeader();
        }

        private boolean header = true;
        private boolean withoutQuote;
        private String[] include;
        private String[] exclude;
        private String[] order = new String[]{};
        private Boolean asc = null;

        private Options() {}

        public Options withoutHeader() {
            this.header = false;
            return this;
        }

        public Options withoutQuote() {
            this.withoutQuote = true;
            return this;
        }

        public Options include(String... include) {
            this.include = include;
            Arrays.sort(this.include);
            return this;
        }

        public Options exclude(String... exclude) {
            this.exclude = exclude;
            Arrays.sort(this.exclude);
            return this;
        }

        public Options order(String... order) {
            this.order = order;
            return this;
        }

        public Options asc() {
            this.asc = true;
            return this;
        }

        public Options desc() {
            this.asc = false;
            return this;
        }
    }
}
