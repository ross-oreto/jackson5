package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;

import static io.oreto.jackson.Util.MultiString;
import static io.oreto.jackson.Util.Str;

class JsonRenderer {
    private final ObjectMapper objectMapper;
    private final boolean pretty;

    /**
     * Constructor
     * @param objectMapper ObjectMapper to use for this renderer
     * @param pretty Pretty print json if true
     */
    JsonRenderer(ObjectMapper objectMapper, boolean pretty) {
        this.objectMapper = objectMapper;
        this.pretty = pretty;
    }

    /**
     * @param objectMapper ObjectMapper to use for this renderer
     */
    JsonRenderer(ObjectMapper objectMapper) {
        this(objectMapper, false);
    }

    /**
     * @return The underlying ObjectReader
     */
    protected ObjectReader reader() {
        return objectMapper.reader();
    }
    /**
     * @return The underlying ObjectWriter
     */
    protected ObjectWriter writer() {
        return objectMapper.writer();
    }

    /**
     * Serialize Object o to JSON
     * @param o The Object to serialize
     * @param pretty Pretty print the JSON if true
     * @return A JSON String
     * @throws JsonProcessingException If errors occur during serialization
     */
    protected String asString(Object o, boolean pretty) throws JsonProcessingException {
        return pretty
                ? writer().withDefaultPrettyPrinter().writeValueAsString(o)
                : writer().writeValueAsString(o);
    }

    /**
     * Render JSON String
     * @param o Object to serialize to JSON
     * @return The JSON String
     * @throws JsonProcessingException If errors occur during serialization
     */
    String render(Object o) throws JsonProcessingException {
        return asString(o, pretty);
    }

    /**
     * Render JSON String
     * @param o Object to serialize to JSON
     * @param fields Fields representing the object fields which are serialized
     * @return The JSON String
     * @throws JsonProcessingException If errors occur during serialization
     */
    String render(Object o, IFields fields) throws JsonProcessingException {
        return asString(json(o, fields), pretty);
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @return JsonNode
     */
    JsonNode json(Object o) throws JsonProcessingException {
        if (o instanceof IFields) {
            return json(o, (IFields) o);
        }
        return objectMapper.valueToTree(o);
    }

    /**
     * Provides cleanup to reduce the number of Json objects and help GC
     * This is helpful when we are using inclusions and create new json nodes in the process.
     * @param json A list of json object nodes.
     */
    private void clearTree(List<ObjectNode> json) {
        json.forEach(this::clearTree);
        json.clear();
    }
    private void clearTree(JsonNode jsonNode) {
        jsonNode.forEach(this::clearTree);
        if (jsonNode instanceof ObjectNode) {
            ((ObjectNode) jsonNode).removeAll();
        } else if (jsonNode instanceof ArrayNode) {
            ((ArrayNode) jsonNode).removeAll();
        }
    }

    /**
     * Convert Object to a JsonNode object
     * @param o Object to convert
     * @param fields Fields representing the object fields which are converted
     * @return JsonNode
     */
    JsonNode json(Object o, IFields fields) throws JsonProcessingException {
        // if root is present, use the specified root.
        if (Str.isNotBlank(fields.root())) {
            o = useRoot(o, fields.root());
        }
        boolean inclusions = Str.isNotBlank(fields.include());
        boolean exclusions = Str.isNotBlank(fields.exclude());
        // if there are no includes or excludes just render normally
        if ((!exclusions || o == null) && !inclusions) {
            return o instanceof ObjectNode ? (JsonNode) o : objectMapper.valueToTree(o);
        } else {
            List<ObjectNode> json = initTree(o);
            if (inclusions && exclusions) {
                // both inclusions and exclusions
                JsonNode copy = json.size() == 1 ? reader().createObjectNode() : reader().createArrayNode();
                walk(json, Str.EMPTY, picker(fields.include()), copy);
                clearTree(json);
                json = new ArrayList<>();
                if (copy instanceof ObjectNode) {
                   json.add((ObjectNode) copy);
                } else {
                   for (JsonNode jsonNode : copy) {
                       if (jsonNode instanceof ObjectNode)
                           json.add((ObjectNode) jsonNode);
                   }
                }
                track(json, Str.EMPTY, picker(fields.exclude()), null);
                return json.size() == 1 ? json.get(0) : toArrayNode(json);
            } else if (inclusions) {
                // only inclusions
                JsonNode copy = json.size() == 1 ? reader().createObjectNode() : reader().createArrayNode();
                walk(json, Str.EMPTY, picker(fields.include()), copy);
                clearTree(json);
                return copy;
            } else {
                // only exclusions
                track(json, Str.EMPTY, picker(fields.exclude()), null);
                return json.size() == 1 ? json.get(0) : toArrayNode(json);
            }
        }
    }

    private ArrayNode toArrayNode(Collection<ObjectNode> nodes) {
        ArrayNode arrayNode = (ArrayNode) reader().createArrayNode();
        for(ObjectNode node : nodes)
            arrayNode.add(node);
        return arrayNode;
    }

    private Object useRoot(Object o, String root) {
        List<JsonNode> picks = new ArrayList<>();

        Object node = objectMapper.valueToTree(o);
        List<ObjectNode> elements = new ArrayList<>();
        if (node instanceof ObjectNode) {
            elements.add((ObjectNode) node);
        } else if (node instanceof ArrayNode) {
            for (JsonNode jsonNode : ((ArrayNode) node)) {
                if (jsonNode instanceof ObjectNode)
                    elements.add((ObjectNode) jsonNode);
            }
            String str = root.trim();
            if (str.startsWith("[") && str.endsWith("]")) {
                String[] range = str.subSequence(1, str.length() - 1).toString().split(":");
                int start = Str.toInteger(range[0]).orElse(1) - 1;
                int end = range.length > 1 ? Str.toInteger(range[1]).orElse(start) : start;
                o = end - start < 2 ? elements.get(start) : elements.subList(start, end);
                root = null;
            }
        } else {
            elements = new ArrayList<>();
        }
        if (Objects.nonNull(root))
            track(elements, Str.EMPTY, picker(root), picks);
        int size = picks.size();
        if (size == 1) o = picks.get(0);
        else if (size > 1) {
            ArrayNode jsonArray = (ArrayNode) reader().createArrayNode();
            picks.forEach(jsonArray::add);
            o = jsonArray;
        }
        return o;
    }

    private List<ObjectNode> initTree(Object o) throws JsonProcessingException {
        List<ObjectNode> json = new ArrayList<>();
        if (o instanceof ObjectNode) {
            ObjectNode element = (ObjectNode) o;
            json.add(element);
        } else if(o instanceof ArrayNode) {
            ArrayNode element = (ArrayNode) o;
            for (JsonNode jsonNode : element) {
                if (jsonNode instanceof ObjectNode)
                    json.add((ObjectNode) jsonNode);
            }
        } else {
            JsonNode element = o instanceof String
                    ? objectMapper.readTree((String) o)
                    : objectMapper.valueToTree(o);
            if (element.isArray()) {
                for (JsonNode jsonNode : element) {
                    if (jsonNode instanceof ObjectNode)
                        json.add((ObjectNode) jsonNode);
                }
            } else {
               json.add((ObjectNode) element);
            }
        }
        return json;
    }

    protected Map<String, List<String>> picker(String dsl) {
        MultiString<String> picks = new MultiString<>();
        StringBuilder sb = new StringBuilder();
        Stack<String> address = new Stack<>();
        String currentAddress = Str.EMPTY;
        int dotted = 0;

        int len = dsl.length();
        for (int i = 0; i < len; i++){
            char c = dsl.charAt(i);
            switch (c) {
                case '{':
                    currentAddress = open(sb, address, picks, currentAddress);
                    dotted = 0;
                    break;
                case '.':
                    currentAddress = open(sb, address, picks, currentAddress);
                    dotted = 1;
                    break;
                case '}':
                    if (dotted > 0) {
                        currentAddress = close(sb, address, picks, currentAddress);
                        dotted = 0;
                    }
                    currentAddress = close(sb, address, picks, currentAddress);
                    break;
                default:
                    if (dotted == 1 && c == ' ') {
                        dotted = 2;
                    } else if (dotted == 2 && c != ' ') {
                        currentAddress = close(sb, address, picks, currentAddress);
                        dotted = 0;
                    }
                    sb.append(c);
                    if (i == len - 1) {
                        addFields(sb.toString(), picks, currentAddress);
                        sb.setLength(0);
                    }
                    break;
            }
        }

        Map<String, Long> hits = new HashMap<>();
        picks.asMap().values().stream().flatMap(Collection::stream)
                .filter(it -> it.startsWith("*")).forEach(it -> {
                    String path = it.substring(1);
                    hits.put(it, picks.asMap().keySet().stream().filter(key -> key.startsWith(path)).count());
                });

        return picks.sort((o1, o2) -> {
            boolean isPointer = hits.containsKey(o1);
            int comp = Boolean.compare(isPointer, hits.containsKey(o2));
            return comp == 0 && isPointer ? hits.get(o1).compareTo(hits.get(o2)) : comp;
        }).asMap();
    }

    protected String open(StringBuilder sb, Stack<String> address, MultiString<String> picks, String currentAddress) {
        List<String> fields = Arrays.asList(sb.toString().trim().split("[ \n\r]"));
        int size = fields.size();
        if (size > 1) {
            for (String field : fields.subList(0, size - 1)) picks.put(currentAddress, field.trim());
        }
        String pointer = fields.get(size - 1).trim();
        address.push(pointer);
        picks.put(currentAddress, "*"+pointer);
        sb.setLength(0);
        return address(address);
    }

    protected String close(StringBuilder sb, Stack<String> address, MultiString<String> picks, String currentAddress) {
        addFields(sb.toString(), picks, currentAddress);
        sb.setLength(0);
        address.pop();
        return address(address);
    }

    protected Map.Entry<String, Subset> parseIndex(String property) {
        if (property.matches(".*\\[[0-9]+]$")) {
            int start = property.indexOf('[');
            String name = property.substring(0, start).trim();
            String index = property.substring(start + 1, property.length() - 1).trim();
            start = Integer.parseInt(index);
            return new AbstractMap.SimpleEntry<>(name, Subset.of(start - 1));
        } else if (property.matches(".*\\[[0-9]*:[0-9]*]$")) {
            int start = property.indexOf('[');
            String name = property.substring(0, start).trim();
            String index = property.substring(start + 1, property.length() - 1).trim();

            String[] range = index.split(":");
            String a = range[0].trim();
            String b = range.length > 1 ? range[1].trim() : Str.EMPTY;
            return new AbstractMap.SimpleEntry<>(name
                    , Subset.of(
                    a.equals(Str.EMPTY) ? null : Integer.parseInt(a) - 1
                    , b.equals(Str.EMPTY) ? null : Integer.parseInt(b) - 1));
        }
        return null;
    }

    private String resolveAddress(String path, String address) {
        return Str.EMPTY.equals(path) ? address : String.format("%s.%s", path, address);
    }
    private String address(Stack<String> stack) {
        return stack.stream().filter(it-> !it.isEmpty()).collect(Collectors.joining("."));
    }
    private void addFields(String fields, MultiString<String> picks, String currentAddress) {
        for (String field : fields.trim().split("[ \n\r]")) {
            picks.put(currentAddress, field.trim());
        }
    }

    protected void track(List<ObjectNode> nodes, String path, Map<String, List<String>> pathMap, List<JsonNode> picks) {
        for (String address : pathMap.get(path)) {
            boolean isPointer = address.startsWith("*");
            address = isPointer ? address.substring(1) : address;
            String property = address;
            Map.Entry<String, Subset> subset = parseIndex(property);
            if (subset != null) { property = subset.getKey(); }

            for (ObjectNode node : nodes) {
                JsonNode element = node.get(property);
                if (isPointer) {
                    List<ObjectNode> newNodes = new ArrayList<>();
                    if (element instanceof ArrayNode) {
                        ArrayNode jsonArray = (ArrayNode) element;
                        if (subset == null)
                            jsonArray.forEach(it -> newNodes.add((ObjectNode) it));
                        else {
                            subset.getValue().size(jsonArray.size()).apply(newNodes, jsonArray);
                        }
                    } else if (element instanceof ObjectNode) {
                        newNodes.add((ObjectNode) element);
                    }
                    track(newNodes, resolveAddress(path, address), pathMap, picks);
                } else {
                    if (element instanceof ArrayNode && subset != null) {
                        ArrayNode jsonArray = (ArrayNode) node.get(property);
                        if (picks == null)
                            subset.getValue().size(jsonArray.size()).remove(jsonArray);
                        else
                            subset.getValue().size(jsonArray.size()).add(picks, jsonArray);
                    } else {
                        if (picks == null)
                            node.remove(property);
                        else
                            picks.add(node.get(property));
                    }
                }
            }
        }
    }

    protected void walk(ObjectNode node, ObjectNode copy
            , String path, Map<String, List<String>> pathMap, List<ObjectNode> newNodes
            , String property, String address) {
        newNodes.add(node);
        ObjectNode newObject = (ObjectNode) reader().createObjectNode();
        copy.set(property, newObject);
        walk(newNodes, resolveAddress(path, address), pathMap, newObject);
    }

    protected void walk(ObjectNode node, ArrayNode copy, String path, Map<String, List<String>> pathMap
            , List<ObjectNode> newNodes, String property, String address) {
        for(JsonNode jsonNode : copy) {
            if (!jsonNode.has(property)) {
                newNodes.add(node);
                ObjectNode newObject = (ObjectNode) reader().createObjectNode();
                ((ObjectNode)jsonNode).set(property, newObject);
                walk(newNodes, resolveAddress(path, address), pathMap, newObject);
                break;
            }
        }
    }

    protected void walk(ArrayNode node, ArrayNode copy
            , String path, Map<String, List<String>> pathMap
            , Map.Entry<String, Subset> subset, List<ObjectNode> newNodes
            , String property, String address) {

        if (subset == null) node.forEach(it -> newNodes.add((ObjectNode) it));
        else subset.getValue().size(node.size()).apply(newNodes, node);

        for(JsonNode jsonNode : copy) {
            if (!jsonNode.has(property)) {
                ArrayNode newArray = (ArrayNode) reader().createArrayNode();
                ((ObjectNode)jsonNode).set(property, newArray);
                walk(newNodes, resolveAddress(path, address), pathMap, newArray);
                break;
            }
        }
    }

    protected void walk(ArrayNode node, ObjectNode copy
            , String path, Map<String, List<String>> pathMap
            , Map.Entry<String, Subset> subset, List<ObjectNode> newNodes
            , String property, String address) {
        if (subset == null) node.forEach(it -> newNodes.add((ObjectNode) it));
        else subset.getValue().size(node.size()).apply(newNodes, node);

        ArrayNode newArray = (ArrayNode) reader().createArrayNode();
        if (copy.has(property)) {
            newArray = (ArrayNode) copy.get(property);
        } else
            copy.set(property, newArray);
        walk(newNodes, resolveAddress(path, address), pathMap, newArray);
    }

    protected void walk(List<ObjectNode> nodes, String path, Map<String, List<String>> pathMap, JsonNode copy) {
        for (ObjectNode node : nodes) {
            List<Map.Entry<String, JsonNode>> elements = pathMap.get(path)
                    .stream().filter(it -> !it.startsWith("*")).map(property -> {
                        Map.Entry<String, Subset> subset = parseIndex(property);
                        if (subset != null) { property = subset.getKey(); }
                        JsonNode element = node.get(property);
                        if (element instanceof ArrayNode && subset != null) {
                            ArrayNode jsonArray = (ArrayNode) element;
                            ArrayNode newArray = (ArrayNode) reader().createArrayNode();
                            subset.getValue().size(jsonArray.size()).apply(newArray, jsonArray);
                            return new AbstractMap.SimpleEntry<String, JsonNode>(property, newArray);
                        } else
                            return new AbstractMap.SimpleEntry<>(property, element);
                    }).filter(it -> !it.getKey().isEmpty()).collect(Collectors.toList());

            if (elements.size() > 0) {
                if (copy instanceof ArrayNode) {
                    addElement((ArrayNode) copy, elements);
                } else if (copy instanceof ObjectNode) {
                    addElement((ObjectNode) copy, elements);
                }
            }
        }

        for (String address : pathMap.get(path)
                .stream().filter(it -> it.startsWith("*")).collect(Collectors.toList())) {
            address = address.substring(1);
            String property = address;
            Map.Entry<String, Subset> subset = parseIndex(property);
            if (subset != null) { property = subset.getKey(); }

            for (ObjectNode node: nodes) {
                if (!node.has(property)) continue;

                JsonNode element = node.get(property);

                if (element instanceof ObjectNode) {
                    if (copy instanceof ObjectNode)
                        walk((ObjectNode) element, (ObjectNode) copy, path, pathMap
                                , new ArrayList<>(), property, address);
                    else if (copy instanceof ArrayNode)
                        walk((ObjectNode) element, (ArrayNode) copy, path, pathMap, new ArrayList<>(), property, address);
                } else if (element instanceof ArrayNode) {
                    if (copy instanceof ObjectNode)
                        walk((ArrayNode) element, (ObjectNode) copy, path, pathMap, subset
                                , new ArrayList<>(), property, address);
                    else if (copy instanceof ArrayNode)
                        walk((ArrayNode) element, (ArrayNode) copy, path, pathMap, subset
                                , new ArrayList<>(), property, address);
                }
            }
        }
    }

    private void addElement(ArrayNode jsonArray, List<Map.Entry<String, JsonNode>> elements) {
        ObjectNode jsonObject = (ObjectNode) reader().createObjectNode();
        elements.forEach(it -> jsonObject.set(it.getKey(), it.getValue()));
        jsonArray.add(jsonObject);
    }

    private void addElement(ObjectNode jsonObject, List<Map.Entry<String, JsonNode>> elements) {
        elements.forEach(it -> addElement(jsonObject, it.getKey(), it.getValue()));
    }

    private void addElement(ObjectNode jsonObject, String property, JsonNode element) {
        if (jsonObject.has(property) && element instanceof ArrayNode)
            ((ArrayNode)jsonObject.get(property)).addAll((ArrayNode) element);
        else
            jsonObject.set(property, element);
    }

    static class Subset {
        static Subset of(Integer a, Integer b) { return new Subset(a, b); }
        static Subset of(Integer a) { return new Subset(a, a); }

        private Integer start, max;
        private final Integer end, min;

        private Subset(Integer a, Integer b) {
            start(a);
            end = b;
            min = max = 0;
        }

        private void start(Integer i) { this.start = i == null ? min : i; }

        Subset size(Integer i) {
            this.max = i - 1; return this;
        }

        void apply(List<ObjectNode> newNodes, ArrayNode node) {
            for (int i = computeStart(); i < computeEnd() + 1; i++) {
                newNodes.add((ObjectNode) node.get(i));
            }
        }
        void apply(ArrayNode newArray, ArrayNode node) {
            for (int i = computeStart(); i < computeEnd() + 1; i++) {
                newArray.add(node.get(i));
            }
        }

        void remove(ArrayNode jsonArray) {
            int start = computeStart();
            for (int i = computeStart(); i < computeEnd() + 1; i++) {
                jsonArray.remove(start);
            }
        }
        void add(List<JsonNode> newNodes, ArrayNode node) {
            int start = computeStart();
            for (int i = computeStart(); i < computeEnd() + 1; i++) {
                newNodes.add(node.get(start));
            }
        }

        int computeStart() { return start < min ? min : start; }
        int computeEnd() { return end == null || end > max ? max : end; }
    }
}