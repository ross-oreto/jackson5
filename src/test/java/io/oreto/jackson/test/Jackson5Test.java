package io.oreto.jackson.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.oreto.jackson.Jackson5;
import io.oreto.jackson.Structure;
import io.oreto.jackson.latte.Lists;
import io.oreto.jackson.test.pojos.Pojo;
import io.oreto.jackson.test.pojos.Pojo1;
import io.oreto.jackson.test.pojos.Pojo3;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Jackson5Test {

    @Test
    public void select1() throws JsonProcessingException {
        Jackson5 jackson5 = Jackson5.build();

        List<Pojo> items = Lists.of(
                new Pojo("test1")
                , new Pojo("test2")
        );

        String json = jackson5.render(items, Structure.of().view("[1]").drop("pojos"));
        assertEquals("{\"name\":\"test1\",\"description\":null}", json);

        assertEquals("[{\"name\":\"test1\",\"description\":null},{\"name\":\"test2\",\"description\":null}]"
                , jackson5.render(items, Structure.of().drop("pojos")));
    }

    @Test
    public void select2() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));
        Jackson5 jackson5 = Jackson5.build();

        assertEquals("{\"name\":\"test1\",\"description\":\"a\"}"
                , jackson5.render(pojos, Structure.of().view("[1]").drop("pojos")));

        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , jackson5.render(pojos, Structure.of().view("[1:2]").select("name").drop("pojos")));
    }

    @Test
    public void select3() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(new Pojo("test1", "a")
                , new Pojo("test2", "b")
                , new Pojo("test3", "c"));

        Jackson5 jackson5 = Jackson5.build();
        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , jackson5.render(pojos, Structure.of().select("description").drop("pojos")));

        assertEquals("[{\"description\":\"a\"},{\"description\":\"b\"},{\"description\":\"c\"}]"
                , jackson5.render(pojos, Structure.of().drop("name pojos")));
    }

    @Test
    public void select4() throws JsonProcessingException {
        List<Pojo> pojos = Lists.of(
                new Pojo("test1", "a")
                , new Pojo("test2", "b")
        );

        Jackson5 jackson5 = Jackson5.build();

        assertEquals("[{\"name\":\"test1\"},{\"name\":\"test2\"}]"
                , jackson5.render(pojos, Structure.of().select("name")));

        assertEquals("{\"description\":\"a\"}"
                , jackson5.render(pojos, Structure.of().view("[1]").select("{ description }")));
    }

    @Test
    public void select5() throws JsonProcessingException {
        List<Pojo> items = Lists.of(
                new Pojo("test1", "a")
                        .withPojos(new Pojo1("a").withPojos("1", "2")
                                , new Pojo1("b").withPojos("3", "4")
                                , new Pojo1("c").withPojos("5", "6"))
                , new Pojo("test2", "b")
                        .withPojos(new Pojo1("d").withPojos("7", "8"), new Pojo1("e"), new Pojo1("f"))
        );

        Jackson5 jackson5 = Jackson5.build();
        assertEquals("[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}," +
                        "{\"name\":\"c\"}]},{\"name\":\"test2\",\"pojos\":[{\"name\":\"d\"},{\"name\":\"e\"},{\"name\":\"f\"}]}]"
                , jackson5.render(items, Structure.of("{\n\rname\npojos{\r\nname\r} \t} ")));

        assertEquals( "[{\"name\":\"test1\",\"pojos\":[{\"name\":\"a\",\"pojos\":" +
                        "[{\"name\":\"1\"},{\"name\":\"2\"}]},{\"name\":\"b\",\"pojos\":" +
                        "[{\"name\":\"3\"},{\"name\":\"4\"}]},{\"name\":\"c\",\"pojos\":" +
                        "[{\"name\":\"5\"},{\"name\":\"6\"}]}]},{\"name\":\"test2\",\"pojos\":" +
                        "[{\"name\":\"d\",\"pojos\":[{\"name\":\"7\"},{\"name\":\"8\"}]},{\"name\":\"e\",\"pojos\":" +
                        "[]},{\"name\":\"f\",\"pojos\":[]}]}]"
                , jackson5.render(items, Structure.of("{ name pojos{ name pojos {name} }}")));
    }

    @Test
    public void select6() throws JsonProcessingException {
        List<Pojo3> items = Lists.of(
                new Pojo3("pojo1", new Pojo("test1"))
                , new Pojo3("pojo2", new Pojo("test2").withPojos("a", "b"))
        );
        Jackson5 jackson5 = Jackson5.build();

        assertEquals("[{\"name\":\"pojo1\",\"pojo\":{\"name\":\"test1\",\"pojos\":[]}}" +
                        ",{\"name\":\"pojo2\",\"pojo\":{\"name\":\"test2\",\"pojos\":[{\"name\":\"a\"},{\"name\":\"b\"}]}}]"
                , jackson5.render(items, Structure.of("{ name pojo { name pojos {name} } }")));
    }

    @Test
    public void simple() throws JsonProcessingException {
        Jackson5 jackson5 = Jackson5.build();
        String json = jackson5.render(new HashMap<String, String>(){{ put("test", "t1"); }});
        assertEquals("{\"test\":\"t1\"}", json);
    }
}
