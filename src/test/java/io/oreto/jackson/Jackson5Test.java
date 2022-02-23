package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.oreto.jackson.pojos.Pojo;
import io.oreto.jackson.pojos.Pojo1;
import io.oreto.jackson.pojos.Pojo3;
import io.oreto.jackson.pojos.PojoDate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Jackson5Test {

    @Test
    public void simple() throws JsonProcessingException {
        Jackson5 jackson5 = Jackson5.get();
        String json = jackson5.string(new HashMap<String, String>(){{ put("test", "t1"); }});
        assertEquals("{\"test\":\"t1\"}", json);
    }

    @Test
    public void test1() throws JsonProcessingException {
        Jackson5 jackson5 = Jackson5.get();

        List<Pojo> items = Lists.of(
                new Pojo("test1")
                , new Pojo("test2")
        );

        String json = jackson5.string(items, Fields.Root("[1]").exclude("pojos"));
        assertEquals("{\"name\":\"test1\",\"description\":null}", json);

        assertEquals("[{\"name\":\"test1\",\"description\":null},{\"name\":\"test2\",\"description\":null}]"
                , jackson5.string(items, Fields.Exclude("pojos")));
    }

    @Test
    public void test2() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));
        Jackson5 jackson5 = Jackson5.get();

        assertEquals("{\"name\":\"test1\",\"description\":\"a\"}"
                , jackson5.string(pojos, Fields.Root("[1]").exclude("pojos")));

        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , jackson5.string(pojos, Fields.Root("[1:2]").include("name").exclude("pojos")));
    }

    @Test
    public void test3() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));

        Jackson5 jackson5 = Jackson5.get();
        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , jackson5.string(pojos, Fields.Include("description").exclude("pojos")));

        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , jackson5.string(pojos, Fields.Exclude("name pojos")));
    }

    @Test
    public void test4() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(
                new Pojo("test1", "a")
                , new Pojo("test2", "b")
        );

        Jackson5 jackson5 = Jackson5.get();

        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , jackson5.string(pojos, Fields.Include("name")));

        assertEquals("{\"description\":\"a\"}"
                , jackson5.string(pojos, Fields.Root("[1]").include("{ description }")));
    }

    @Test
    public void test5() throws JsonProcessingException {
        List<Pojo> items = Lists.of(
                new Pojo("test1", "a")
                        .withPojos(new Pojo1("a").withPojos("1", "2")
                                , new Pojo1("b").withPojos("3", "4")
                                , new Pojo1("c").withPojos("5", "6"))
                , new Pojo("test2", "b")
                        .withPojos(new Pojo1("d").withPojos("7", "8"), new Pojo1("e"), new Pojo1("f"))
        );

        Jackson5 jackson5 = Jackson5.get();
        assertEquals("[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}," +
                        "{\"name\":\"c\"}]},{\"name\":\"test2\",\"pojos\":[{\"name\":\"d\"},{\"name\":\"e\"},{\"name\":\"f\"}]}]"
                , jackson5.string(items, Fields.Include("{\n\rname\npojos{\r\nname\r} \t} ")));

        assertEquals( "[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\",\"pojos\":" +
                        "[{\"name\":\"1\"},{\"name\":\"2\"}]},{\"name\":\"b\",\"pojos\":" +
                        "[{\"name\":\"3\"},{\"name\":\"4\"}]},{\"name\":\"c\",\"pojos\":" +
                        "[{\"name\":\"5\"},{\"name\":\"6\"}]}]},{\"name\":\"test2\",\"pojos\":" +
                        "[{\"name\":\"d\",\"pojos\":[{\"name\":\"7\"},{\"name\":\"8\"}]},{\"name\":\"e\",\"pojos\":" +
                        "[]},{\"name\":\"f\",\"pojos\":[]}]}]"
                , jackson5.string(items, Fields.Include("{ name pojos{ name pojos {name} }}")));
    }

    @Test
    public void test6() throws JsonProcessingException {
        List<Pojo3> items = Lists.of(
                new Pojo3("pojo1", new Pojo("test1"))
                , new Pojo3("pojo2", new Pojo("test2").withPojos("a", "b"))
        );
        Jackson5 jackson5 = Jackson5.get();

        String json = jackson5.string(items, Fields.Include("{ name pojo { name pojos {name} } }"));
        assertEquals("[{\"name\":\"pojo1\",\"pojo\":{\"name\":\"test1\",\"pojos\":[]}}" +
                        ",{\"name\":\"pojo2\",\"pojo\":{\"name\":\"test2\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}]}}]"
                , json);
    }

    @Test
    public void test7() throws JsonProcessingException {
        List<Pojo3> items = Lists.of(
                new Pojo3("pojo1", new Pojo("test1"))
                , new Pojo3("pojo2", new Pojo("test2").withPojos("a", "b"))
        );
        Jackson5 jackson5 = Jackson5.get();

        assertEquals("[{\"name\":\"pojo1\",\"pojo\":{\"name\":\"test1\",\"description\":null,\"pojos\":[]}}," +
                        "{\"name\":\"pojo2\",\"pojo\":{\"name\":\"test2\",\"description\":null,\"pojos\":" +
                        "[{\"description\":null,\"pojos\":[]},{\"description\":null,\"pojos\":[]}]}}]"
                , jackson5.string(items, Fields.Exclude("{ pojo { pojos {name} } }")));
    }

    @Test
    public void test8() throws JsonProcessingException {
        Pojo pojo = new Pojo("test2", "b")
                .withPojos(new Pojo1("d").withPojos("7", "8", "9", "10", "11", "12")
                        , new Pojo1("e")
                        , new Pojo1("f"));

        Jackson5 jackson5 = Jackson5.get();
        String json = jackson5.string(pojo, Fields.Include("name pojos[1] { name pojos[2:4] }"));
        assertEquals("{\"name\":\"test2\",\"pojos\":[{\"name\":\"d\",\"pojos\":[{\"name\":\"8\"},{\"name\":\"9\"},{\"name\":\"10\"}]}]}"
                ,json);
    }

    @Test
    public void test9() throws JsonProcessingException {
        Pojo pojo = new Pojo("test2", "b")
                .withPojos(new Pojo1("d").withPojos("7", "8", "9", "10", "11", "12")
                        , new Pojo1("e", "foo")
                        , new Pojo1("f", "bar"));

        Jackson5 jackson5 = Jackson5.get();
        String json = jackson5.string(pojo, Fields.Exclude("name pojos[1] { name pojos[2:4] }"));
        assertEquals("{\"description\":\"b\",\"pojos\":[{\"description\":null,\"pojos\":[{\"name\":\"7\"},{\"name\":\"11\"},{\"name\":\"12\"}]},{\"name\":\"e\",\"description\":\"foo\",\"pojos\":[]},{\"name\":\"f\",\"description\":\"bar\",\"pojos\":[]}]}"
                ,json);
    }

    @Test
    public void test10() throws IOException {
        PojoDate pojoDate = new PojoDate();
        pojoDate.setLocalDateTime(LocalDateTime.now());
        pojoDate.setLocalDate(LocalDate.now());
        Date date = new Date();
        pojoDate.setDate(date);
        pojoDate.setSqlDate(new java.sql.Date(date.getTime()));

        Jackson5 jackson5 = Jackson5.get();

        String json = jackson5.string(pojoDate);
        PojoDate pojoDate1 = jackson5.object(json, PojoDate.class);

        assertEquals(jackson5.string(pojoDate.getDate()), jackson5.string(pojoDate1.getDate()));
    }

    @Test
    public void test11() throws JsonProcessingException {
        PojoDate pojoDate = new PojoDate();
        LocalDate localDate = LocalDate.of(2022, 2, 11);
        LocalDateTime localDateTime =
                LocalDateTime.of(2022, 2, 11, 23, 36, 0);
        pojoDate.setLocalDate(localDate);
        pojoDate.setLocalDateTime(localDateTime);
        pojoDate.setDate(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        pojoDate.setSqlDate(new java.sql.Date(pojoDate.getDate().getTime()));

        JsonNode json = Jackson5.get().json(pojoDate);
        assertEquals("02-11-2022", json.get("localDate").asText());
        assertEquals("02-11-2022 23:36:00", json.get("localDateTime").asText());
        assertEquals("02-11-2022 00:00:00", json.get("date").asText());
        assertEquals("02-11-2022 00:00:00", json.get("sqlDate").asText());
    }

    @Test
    public void test12() throws JsonProcessingException {
        PojoDate pojoDate = new PojoDate();
        LocalDate localDate = LocalDate.of(2022, 2, 11);
        LocalDateTime localDateTime =
                LocalDateTime.of(2022, 2, 11, 23, 36, 0);
        pojoDate.setLocalDate(localDate);
        pojoDate.setLocalDateTime(localDateTime);
        pojoDate.setDate(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        pojoDate.setSqlDate(new java.sql.Date(pojoDate.getDate().getTime()));
        pojoDate.setTime(new java.sql.Time(pojoDate.getDate().getTime()));

        Jackson5.supply("j5", MapperConfig.defaultConfig()
                .dateFormat("MM/dd/yyyy")
                .timeFormat("HH:mm")
                .dateTimeFormat("MM/dd/yyyy HH:mm"));
        Jackson5 jackson5 = Jackson5.find("j5").orElseThrow(() -> new RuntimeException("can't find Jackson5"));

        JsonNode json = jackson5.json(pojoDate);
        assertEquals("02/11/2022", json.get("localDate").asText());
        assertEquals("02/11/2022 23:36", json.get("localDateTime").asText());
        assertEquals("02/11/2022 23:36", json.get("date").asText());
        assertEquals("02/11/2022 23:36", json.get("sqlDate").asText());
        assertEquals("23:36", json.get("time").asText());
    }

    @Test
    public void test13() throws IOException {
        Jackson5 jackson5 = Jackson5.get();
        String json = "{ 'name': 'test', 'description': 'description' }";
        JsonNode jsonNode =
                jackson5.json(json, Fields.Include("name"));
        assertEquals("test", jsonNode.get("name").asText());
        assertEquals(1, jsonNode.size());

        Pojo pojo = jackson5.object(new HashMap<String, Object>(){{ put("name", "test"); }}, Pojo.class);
        assertEquals("test", pojo.getName());

        Map<String, Object> map = jackson5.map(json);
        assertEquals(jsonNode.get("name").asText(), map.get("name"));

        String test = jackson5.string(json, Fields.Include("name"));
        assertEquals("{\"name\":\"test\"}", test);

        Pojo pojo1 = jackson5.object(json, Pojo.class);
        assertEquals("test", pojo1.getName());
        assertEquals("description", pojo1.getDescription());
    }
}
