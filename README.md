### jackson5 
A json renderer to serialize json dynamically.

Why?
 - It often becomes tiresome and tedious to add custom serializers, jackson views, DTOs, etc. 
It would be nice to just dynamically serialize json according to what the client wants. 
Jackson5 does just that by providing a DSL to specify the json structure.
  
### Requirements
 - jdk 8+
 - maven 3.3.1+

### Usage
 - get a new Jackson5 and render as normal json
```
Jackson5 jackson5 = Jackson5.build();
Map data = new HashMap<String, String>(){{ put("name", "Ryzen5"); put("description", "A CPU"); }};
String json = jackson5.render(data);
```

### Structure DSL
- The structure language is designed almost identically to graphQL syntax. 
- include the name field.
```
String json = jackson5.render(data, "{ name }");
```
- exclude the name field.
```
jackson5.render(data, Fields.Drop("name"))
```

For more advanced examples, look at src/test/io/oreto/jackson/Jackson5Test

Jackson 5 also support CSV. src/test/io/oreto/jackson/CsvTest

