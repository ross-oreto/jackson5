![](https://i.giphy.com/media/pUeXcg80cO8I8/giphy.webp)

### jackson5 
A json renderer to serialize json dynamically.

Why?
 - It often becomes tiresome and tedious to add custom serializers, jackson views, DTOs, etc.
 - JSON objects are already DTOs in their own right. 
 - It would be nice to just dynamically serialize json according to what the client wants. 
Jackson5 does just that by providing a DSL to specify the json structure.
  
### Requirements
 - jdk 8+
 - maven 3+

### Install
```
<dependency>
  <groupId>io.github.ross-oreto.jackson5</groupId>
  <artifactId>jackson5</artifactId>
  <version>1.0.1</version>
</dependency>
```

### The Jackson5 class
- Firstly this class is a static container for ObjectMappers. Map<name -> ObjectMapper>
- Jackson5 preserves thread safety by using the ObjectMapper reader()/writer() methods and not calling any config methods post creation.
- It also provides a handy way to create new object mappers using a fluent MapperConfig object

Get a new Jackson5 using the get() method. Jackson5 will use its own ObjectMapper unless you supply it with one.
```
Jackson5 jackson5 = Jackson5.get();
```
If your application already has an ObjectMapper that you want to use like a Spring Application with a managed bean.
```
@Autowired
ObjectMapper objectMapper;

Jackson5.supply(() -> objectMapper); 
Jackson5 jackson5 = Jackson5.get();
```
Or create new Jackson5 objects by specifying a name.
``` 
Jackson5.supply("foo", () -> objectMapper);
Jackson5 jackson5 = Jackson5.get("foo");
```
Using MapperConfig to create a new ObjectMapper with different date time patterns
```
Jackson5.supply("j5", MapperConfig.defaultConfig()
        .dateFormat("MM/dd/yyyy")
        .timeFormat("HH:mm")
        .dateTimeFormat("MM/dd/yyyy HH:mm"));
Jackson5 jackson5 = Jackson5.get("j5");
```

### Serialization Usage
 - get a new Jackson5 and serialize as normal json
```
Jackson5 jackson5 = Jackson5.get();
Map data = new HashMap<String, String>(){{ put("name", "Ryzen5"); put("description", "A CPU"); }};
String json = jackson5.string(data);
```
as JSON object
```
JsonNode json = jackson5.json(data);
```
as a Map<String, Object>
```
Pojo pojo = new Pojo();
Map<String, Object> json = jackson5.map(pojo); 
```

### Deserialization
``` 
Pojo pojo = jackson5.object(data, Pojo.class);
```

### Fields DSL
- The Fields object DSL is designed almost identically to graphQL syntax. 
- include the name field.
```
String json = jackson5.string(data, "{ name }");
```
- exclude the name field.
```
jackson5.string(data, Fields.Exclude("name"));
```
- Only render first 10 elements
```
jackson5.string(data, "list[1:10]");
```
- Change the root of the tree. If say the data of interest is down in class hierarchy
```
jackson5.string(data, Fields.Root("content.people").include("name address"));
```
- Chaining together (This will look at only the first element in the collection)
```
jackson5.string(data, Fields.Root("[1]").include("{ name address { street state }}"));
```

For more advanced examples, look at src/test/io/oreto/jackson/Jackson5Test

Jackson 5 also supports CSV. src/test/io/oreto/jackson/CsvTest
``` 
List<Map<String, Object>> elements = new ArrayList<>();
elements.add(new LinkedHashMap<String, Object>(){{ put("name", "ross"); put("address", "Nashville, TN"); }});
String csv = Csv.asCsv(elements);
```