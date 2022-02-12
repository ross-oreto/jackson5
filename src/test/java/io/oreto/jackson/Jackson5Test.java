package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Jackson5Test {

    @Test
    public void simple() throws JsonProcessingException {
        Jackson5 jackson5 = Jackson5.get();
        String json = jackson5.render(new HashMap<String, String>(){{ put("test", "t1"); }});
        assertEquals("{\"test\":\"t1\"}", json);
    }

    @Test
    public void test1() throws JsonProcessingException {
        Jackson5 jackson5 = Jackson5.get();

        List<Pojo> items = Lists.of(
                new Pojo("test1")
                , new Pojo("test2")
        );

        String json = jackson5.render(items, Fields.Root("[1]").exclude("pojos"));
        assertEquals("{\"name\":\"test1\",\"description\":null}", json);

        assertEquals("[{\"name\":\"test1\",\"description\":null},{\"name\":\"test2\",\"description\":null}]"
                , jackson5.render(items, Fields.Exclude("pojos")));
    }

    @Test
    public void test2() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));
        Jackson5 jackson5 = Jackson5.get();

        assertEquals("{\"name\":\"test1\",\"description\":\"a\"}"
                , jackson5.render(pojos, Fields.Root("[1]").exclude("pojos")));

        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , jackson5.render(pojos, Fields.Root("[1:2]").include("name").exclude("pojos")));
    }

    @Test
    public void test3() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));

        Jackson5 jackson5 = Jackson5.get();
        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , jackson5.render(pojos, Fields.Include("description").exclude("pojos")));

        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , jackson5.render(pojos, Fields.Exclude("name pojos")));
    }

    @Test
    public void test4() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(
                new Pojo("test1", "a")
                , new Pojo("test2", "b")
        );

        Jackson5 jackson5 = Jackson5.get();

        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , jackson5.render(pojos, Fields.Include("name")));

        assertEquals("{\"description\":\"a\"}"
                , jackson5.render(pojos, Fields.Root("[1]").include("{ description }")));
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
                , jackson5.render(items, Fields.Include("{\n\rname\npojos{\r\nname\r} \t} ")));

        assertEquals( "[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\",\"pojos\":" +
                        "[{\"name\":\"1\"},{\"name\":\"2\"}]},{\"name\":\"b\",\"pojos\":" +
                        "[{\"name\":\"3\"},{\"name\":\"4\"}]},{\"name\":\"c\",\"pojos\":" +
                        "[{\"name\":\"5\"},{\"name\":\"6\"}]}]},{\"name\":\"test2\",\"pojos\":" +
                        "[{\"name\":\"d\",\"pojos\":[{\"name\":\"7\"},{\"name\":\"8\"}]},{\"name\":\"e\",\"pojos\":" +
                        "[]},{\"name\":\"f\",\"pojos\":[]}]}]"
                , jackson5.render(items, Fields.Include("{ name pojos{ name pojos {name} }}")));
    }

    @Test
    public void test6() throws JsonProcessingException {
        List<Pojo3> items = Lists.of(
                new Pojo3("pojo1", new Pojo("test1"))
                , new Pojo3("pojo2", new Pojo("test2").withPojos("a", "b"))
        );
        Jackson5 jackson5 = Jackson5.get();

        assertEquals("[{\"name\":\"pojo1\",\"pojo\":{\"name\":\"test1\",\"pojos\":[]}}" +
                        ",{\"name\":\"pojo2\",\"pojo\":{\"name\":\"test2\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}]}}]"
                , jackson5.render(items, Fields.Include("{ name pojo { name pojos {name} } }")));
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
                , jackson5.render(items, Fields.Exclude("{ pojo { pojos {name} } }")));
    }

    @Test
    public void test8() throws JsonProcessingException {
        Pojo pojo = new Pojo("test2", "b")
                .withPojos(new Pojo1("d").withPojos("7", "8", "9", "10", "11", "12")
                        , new Pojo1("e")
                        , new Pojo1("f"));

        Jackson5 jackson5 = Jackson5.get();
        String json = jackson5.render(pojo, Fields.Include("name pojos[1] { name pojos[2:4]{name} }"));
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
        String json = jackson5.render(pojo, Fields.Exclude("name pojos[1] { name pojos[2:4] }"));
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

        Jackson5.setDefaultTimePattern("hh:mm:ss a");
        Jackson5 jackson5 = Jackson5.get();

        String json = jackson5.render(pojoDate);
        PojoDate pojoDate1 = Jackson5.from(json, PojoDate.class);

        assertEquals(jackson5.render(pojoDate.getDate()), jackson5.render(pojoDate1.getDate()));
    }

    @Test
    public void test11() {
        PojoDate pojoDate = new PojoDate();
        LocalDate localDate = LocalDate.of(2022, 2, 11);
        LocalDateTime localDateTime =
                LocalDateTime.of(2022, 2, 11, 23, 36, 0);
        pojoDate.setLocalDate(localDate);
        pojoDate.setLocalDateTime(localDateTime);
        pojoDate.setDate(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        pojoDate.setSqlDate(new java.sql.Date(pojoDate.getDate().getTime()));

        assertEquals("2022-02-11", Jackson5.get().json(pojoDate).get("localDate").asText());
        assertEquals("2022-02-11 23:36:00", Jackson5.get().json(pojoDate).get("localDateTime").asText());
        assertEquals("2022-02-11 00:00:00", Jackson5.get().json(pojoDate).get("date").asText());
        assertEquals("2022-02-11 00:00:00", Jackson5.get().json(pojoDate).get("sqlDate").asText());
    }

    @Test
    public void test12() {
        PojoDate pojoDate = new PojoDate();
        LocalDate localDate = LocalDate.of(2022, 2, 11);
        LocalDateTime localDateTime =
                LocalDateTime.of(2022, 2, 11, 23, 36, 0);
        pojoDate.setLocalDate(localDate);
        pojoDate.setLocalDateTime(localDateTime);
        pojoDate.setDate(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        pojoDate.setSqlDate(new java.sql.Date(pojoDate.getDate().getTime()));

        Jackson5.setDefaultMapper(Jackson5.registerTimeModule(new ObjectMapper()
                , "MM/dd/yyyy"
                , "MM/dd/yyyy HH:mm"));

        Jackson5 jackson5 = Jackson5.get();
        assertEquals("02/11/2022", jackson5.json(pojoDate).get("localDate").asText());
        assertEquals("02/11/2022 23:36", jackson5.json(pojoDate).get("localDateTime").asText());
        assertEquals("02/11/2022 23:36", jackson5.json(pojoDate).get("date").asText());
        assertEquals("02/11/2022 23:36", jackson5.json(pojoDate).get("sqlDate").asText());
    }
}
