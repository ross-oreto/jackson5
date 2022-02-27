package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

import static io.oreto.jackson.Util.Str;

/**
 * Class to convert Fields DSL into a JsonNode which adheres to inclusions/exclusions specified in the DSL
 */
class FieldsDSL {
    private static final Map<String, MultiMap> dslCache = new HashMap<>();
    private static final Map<String, Map.Entry<String, Subset>> indexCache = new HashMap<>();

    private final ObjectMapper mapper;

    /**
     * Constructor
     * @param mapper ObjectMapper to use for this renderer
     */
    FieldsDSL(ObjectMapper mapper) {
        this.mapper = mapper;
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
        return mapper.valueToTree(o);
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
            return o instanceof ObjectNode ? (JsonNode) o : mapper.valueToTree(o);
        } else {
            List<ObjectNode> json = initTree(o);
            if (inclusions && exclusions) {
                // both inclusions and exclusions
                prune(json, Str.EMPTY, selector(fields.include()), true);
                prune(json, Str.EMPTY, selector(fields.exclude()), false);
                return json.size() == 1 ? json.get(0) : toArrayNode(json);
            } else if (inclusions) {
                // only inclusions
                prune(json, Str.EMPTY, selector(fields.include()), true);
                return json.size() == 1 ? json.get(0) : toArrayNode(json);
            } else {
                // only exclusions
                prune(json, Str.EMPTY, selector(fields.exclude()), false);
                return json.size() == 1 ? json.get(0) : toArrayNode(json);
            }
        }
    }

    /**
     * Convert a collection of ObjectNode into a ArrayNode
     * @param nodes The collection of object nodes
     * @return The resulting ArrayNode
     */
    private ArrayNode toArrayNode(Collection<ObjectNode> nodes) {
        ArrayNode arrayNode = (ArrayNode) mapper.reader().createArrayNode();
        for(ObjectNode node : nodes)
            arrayNode.add(node);
        nodes.clear();
        return arrayNode;
    }

    /**
     * Change the JSON tree root
     * @param o The object to convert to json
     * @param root The new root of the tree
     * @return The resulting JSON tree starting at the specified root
     */
    private Object useRoot(Object o, String root) {
        List<JsonNode> nodes = new ArrayList<>();

        Object node = mapper.valueToTree(o);
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
            gather(elements, Str.EMPTY, selector(root), nodes);
        int size = nodes.size();
        if (size == 1) o = nodes.get(0);
        else if (size > 1) {
            ArrayNode jsonArray = (ArrayNode) mapper.reader().createArrayNode();
            nodes.forEach(jsonArray::add);
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
                    ? mapper.readTree((String) o)
                    : mapper.valueToTree(o);
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

    /**
     * Convert the DSL string into a Map representing the path and the selected fields
     * @param dsl The DSL string
     * @return A multi map @{code Map<String, SelectedFields>}
     */
    protected MultiMap selector(String dsl) {
        if (dslCache.containsKey(dsl))
            return dslCache.get(dsl);
        MultiMap pathMap = new MultiMap();
        StringBuilder sb = new StringBuilder();
        Stack<String> address = new Stack<>();
        String currentAddress = Str.EMPTY;
        int dotted = 0;

        int len = dsl.length();
        for (int i = 0; i < len; i++){
            char c = dsl.charAt(i);
            switch (c) {
                case '{':
                    currentAddress = open(sb, address, pathMap, currentAddress);
                    dotted = 0;
                    break;
                case '.':
                    currentAddress = open(sb, address, pathMap, currentAddress);
                    dotted = 1;
                    break;
                case '}':
                    if (dotted > 0) {
                        currentAddress = close(sb, address, pathMap, currentAddress);
                        dotted = 0;
                    }
                    currentAddress = close(sb, address, pathMap, currentAddress);
                    break;
                default:
                    if (dotted == 1 && c == ' ') {
                        dotted = 2;
                    } else if (dotted == 2 && c != ' ') {
                        currentAddress = close(sb, address, pathMap, currentAddress);
                        dotted = 0;
                    }
                    sb.append(c);
                    if (i == len - 1) {
                        addFields(sb.toString(), pathMap, currentAddress);
                        sb.setLength(0);
                    }
                    break;
            }
        }
        dslCache.put(dsl, pathMap);
        return pathMap;
    }

    /**
     * Handle an open bracket level change from parent to child
     * @param sb The current string of fields
     * @param address The address of the nearest parent
     * @param pathMap Map representing the path and the selected fields
     * @param currentAddress The address or path of the current fields
     * @return The new address after opening
     */
    protected String open(StringBuilder sb, Stack<String> address, MultiMap pathMap, String currentAddress) {
        List<String> fields = Arrays.asList(sb.toString().trim().split("[ \n\r]"));
        int size = fields.size();
        if (size > 1) {
            for (int i = 0; i < size - 1; i++)
                pathMap.put(currentAddress, fields.get(i).trim());
        }
        FieldObject fieldObject = FieldObject.parent(fields.get(size - 1).trim());
        address.push(fieldObject.field);
        pathMap.put(currentAddress, fieldObject);
        sb.setLength(0);
        return address(address);
    }

    /**
     * Handle a closing bracket level change from parent to child
     * @param sb The current string of fields
     * @param address The address of the nearest parent
     * @param pathMap Map representing the path and the selected fields
     * @param currentAddress The address or path of the current fields
     * @return The new address after closing
     */
    protected String close(StringBuilder sb, Stack<String> address, MultiMap pathMap, String currentAddress) {
        addFields(sb.toString(), pathMap, currentAddress);
        sb.setLength(0);
        address.pop();
        return address(address);
    }

    private String resolveAddress(String path, String address) {
        return Str.EMPTY.equals(path) ? address : String.format("%s.%s", path, address);
    }

    private String address(Stack<String> stack) {
        List<String> address = new ArrayList<>();
        for (String s : stack) {
            if (!s.isEmpty())
                address.add(s);
        }
        return String.join(".", address);
    }

    private void addFields(String fields, MultiMap pathMap, String currentAddress) {
        for (String field : fields.trim().split("[ \n\r]")) {
            if (field.contains("["))
                pathMap.put(currentAddress, FieldObject.of(field.trim()));
            else
                pathMap.put(currentAddress, field.trim());
        }
    }

    /**
     * Gather all the branches which should be part of the root
     * @param nodes The current root node or nodes
     * @param path The path to of the current fields
     * @param pathMap Map representing the path and the selected fields
     * @param branches The new root node or nodes
     */
    protected void gather(List<ObjectNode> nodes
            , String path
            , MultiMap pathMap
            , List<JsonNode> branches) {
        SelectedFields selectedFields = pathMap.get(path);
        Map<String, String> properties = selectedFields.properties;

        for (ObjectNode node : nodes) {
            properties.forEach((k,v)-> branches.add(node.get(k)));
        }
        for (FieldObject address : selectedFields.objects.values()) {
            Map.Entry<String, Subset> subset = address.subset;
            for (ObjectNode node : nodes) {
                JsonNode element = node.get(address.field);
                if (address.parent) {
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
                    gather(newNodes, resolveAddress(path, address.field), pathMap, branches);
                } else {
                    if (element instanceof ArrayNode && subset != null) {
                        ArrayNode jsonArray = (ArrayNode) node.get(address.field);
                        subset.getValue().size(jsonArray.size()).add(branches, jsonArray);
                    }
                }
            }
        }
    }

    /**
     * Prunes the tree according to the pathMap selected fields
     * @param nodes The nodes being pruned
     * @param path The path to of the current fields
     * @param pathMap Map representing the path and the selected fields
     * @param include True if the fields are being included, otherwise excluded
     */
    protected void prune(List<ObjectNode> nodes
            , String path
            , MultiMap pathMap
            , boolean include) {
        SelectedFields selectedFields = pathMap.get(path);
        Map<String, String> properties = selectedFields.properties;

        for (ObjectNode node : nodes) {
            if (include) {
                List<String> compliment = new ArrayList<>();
                for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
                    String name = it.next();
                    if (!selectedFields.contains(name))
                        compliment.add(name);
                }
                node.remove(compliment);
            } else {
                node.remove(properties.keySet());
            }
        }
        for (FieldObject address : selectedFields.objects.values()) {
            Map.Entry<String, Subset> subset = address.subset;
            for (ObjectNode node : nodes) {
                JsonNode element = node.get(address.field);
                if (address.parent) {
                    List<ObjectNode> newNodes = new ArrayList<>();
                    if (element instanceof ArrayNode) {
                        ArrayNode jsonArray = (ArrayNode) element;
                        if (subset == null)
                            jsonArray.forEach(it -> newNodes.add((ObjectNode) it));
                        else {
                            subset.getValue().size(jsonArray.size()).apply(newNodes, jsonArray);
                            if (include)
                                subset.getValue().size(jsonArray.size()).removeCompliment(jsonArray);
                        }
                    } else if (element instanceof ObjectNode) {
                        newNodes.add((ObjectNode) element);
                    }
                    prune(newNodes, resolveAddress(path, address.field), pathMap, include);
                } else {
                    if (element instanceof ArrayNode && subset != null) {
                        ArrayNode jsonArray = (ArrayNode) node.get(address.field);
                        if (include)
                            subset.getValue().size(jsonArray.size()).removeCompliment(jsonArray);
                        else
                            subset.getValue().size(jsonArray.size()).remove(jsonArray);
                    }
                }
            }
        }
    }

    static class MultiMap {
        private final Map<String, SelectedFields> map = new LinkedHashMap<>();

        final void put(String k, String v) {
            if (map.containsKey(k)) map.get(k).addProperty(v);
            else map.put(k, new SelectedFields() {{
                addProperty(v);
            }});
        }
        final void put(String k, FieldObject v) {
            if (map.containsKey(k)) map.get(k).addObject(v);
            else map.put(k, new SelectedFields() {{
                addObject(v);
            }});
        }
        final SelectedFields get(String k) {
            return map.get(k);
        }
    }

    static class SelectedFields {
        final LinkedHashMap<String, String> properties;
        final LinkedHashMap<String, FieldObject> objects;

        SelectedFields() {
           properties = new LinkedHashMap<>();
           objects = new LinkedHashMap<>();
        }
        void addProperty(String property) {
            properties.put(property, property);
        }
        void addObject(FieldObject fieldObject) {
            objects.put(fieldObject.field, fieldObject);
        }
        boolean contains(String field) {
           return properties.containsKey(field) || objects.containsKey(field);
        }
    }

    static class FieldObject {
        static FieldObject of(String field) {
            return new FieldObject(field, false);
        }
        static FieldObject parent(String field) {
            return new FieldObject(field, true);
        }

        final String field;
        final Map.Entry<String, Subset> subset;
        final boolean parent;

        private FieldObject(String field, boolean parent) {
            this.subset = parseIndex(field);
            this.field = subset == null ? field : subset.getKey();
            this.parent = parent;
        }

        protected Map.Entry<String, Subset> parseIndex(String property) {
            if (indexCache.containsKey(property))
                return indexCache.get(property);

            Map.Entry<String, Subset> subsetEntry = null;
            if (property.matches(".*\\[[0-9]+]$")) {
                int start = property.indexOf('[');
                String name = property.substring(0, start).trim();
                String index = property.substring(start + 1, property.length() - 1).trim();
                start = Integer.parseInt(index);
                subsetEntry = new AbstractMap.SimpleEntry<>(name, Subset.of(start - 1));
            } else if (property.matches(".*\\[[0-9]*:[0-9]*]$")) {
                int start = property.indexOf('[');
                String name = property.substring(0, start).trim();
                String index = property.substring(start + 1, property.length() - 1).trim();

                String[] range = index.split(":");
                String a = range[0].trim();
                String b = range.length > 1 ? range[1].trim() : Str.EMPTY;
                subsetEntry = new AbstractMap.SimpleEntry<>(name
                        , Subset.of(
                        a.equals(Str.EMPTY) ? null : Integer.parseInt(a) - 1
                        , b.equals(Str.EMPTY) ? null : Integer.parseInt(b) - 1));
            }
            indexCache.put(property, subsetEntry);
            return subsetEntry;
        }
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
            int start = computeStart();
            int end = computeEnd() + 1;
            for (int i = start; i < end; i++) {
                newNodes.add((ObjectNode) node.get(i));
            }
        }

        void add(List<JsonNode> newNodes, ArrayNode node) {
            int start = computeStart();
            int end = computeEnd() + 1;
            for (int i = computeStart(); i < end; i++) {
                newNodes.add(node.get(start));
            }
        }

        void remove(ArrayNode jsonArray) {
            int start = computeStart();
            int end = computeEnd() + 1;
            for (int i = computeStart(); i < end; i++) {
                jsonArray.remove(start);
            }
        }

        void removeCompliment(ArrayNode jsonArray) {
            int end = computeEnd() + 1;
            int size = jsonArray.size();
            for (int i = end; i < size; i++) {
                jsonArray.remove(end);
            }
            int start = computeStart();
            for (int i = 0; i < start; i++) {
                jsonArray.remove(0);
            }
        }

        int computeStart() { return start < min ? min : start; }
        int computeEnd() { return end == null || end > max ? max : end; }
    }
}