package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import io.oreto.jackson.models.Account;
import io.oreto.jackson.models.Item;
import io.oreto.jackson.models.Person;
import io.oreto.jackson.models.Purchase;
import io.oreto.jackson.pojos.PojoDate;
import io.oreto.jackson.util.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class Jackson5Test {
    static final List<Person> people = TestUtils.randomPeople(20);

    static Jackson5 jackson5 = Jackson5.get();

    @Test
    public void badInput() throws IOException {
        assertEquals("null", jackson5.serialize(null, ""));
        assertEquals("null", jackson5.serialize(null, Fields.Include("name")));
        assertEquals(NullNode.getInstance(), jackson5.json(null, Fields.Include("name")));
    }

    @Test
    public void serializeMap() throws JsonProcessingException {
        String json = jackson5.serialize(new HashMap<String, String>(){{ put("test", "t1"); }});
        assertEquals("{\"test\":\"t1\"}", json);
    }

    @Test
    public void deserializeMap() throws IOException {
        Map<String, Object> map = jackson5.map("{\"test\":\"t1\"}");
        assertEquals(new HashMap<String, Object>(){{ put("test", "t1"); }}, map);
    }

    @Test
    public void simpleExclude() throws JsonProcessingException {
       JsonNode jsonNode = jackson5.json(people, Fields.Exclude("firstName"));
       jsonNode.forEach(it -> assertFalse(it.has("firstName") && it.size() > 1));
    }

    @Test
    public void simpleInclude() throws JsonProcessingException {
        JsonNode jsonNode = jackson5.json(people, "firstName");
        jsonNode.forEach(it -> assertTrue(it.has("firstName") && it.size() == 1));
    }

    @Test
    public void excludePurchaseItemNames() throws JsonProcessingException {
        JsonNode jsonNode = jackson5.json(people, Fields.Exclude("purchases.items.name"));
        jsonNode.forEach(person -> person.get("purchases")
                .forEach(purchase -> purchase.get("items")
                        .forEach(item -> assertFalse(item.has("name")))));
    }

    @Test
    public void includePurchaseItemNames() throws JsonProcessingException {
        JsonNode jsonNode = jackson5.json(people, Fields.Include("purchases.items.name"));
        jsonNode.forEach(person -> person.get("purchases")
                .forEach(purchase -> purchase.get("items")
                        .forEach(item -> assertTrue(item.has("name") && item.size() == 1))));
    }

    @Test
    public void excludePurchaseItemData() throws IOException {
        List<Person> personList =
                jackson5.convertCollection(people, Person.class, Fields.Exclude("purchases.items{ name price }"));
        personList.forEach(person -> person.getPurchases().forEach(purchase -> purchase.getItems().forEach(item ->
                assertTrue(item.getName() == null && item.getPrice() == null))));
    }

    @Test
    public void includePurchaseItemData() throws IOException {
        List<Person> personList =
                jackson5.convertCollection(people, Person.class, Fields.Include("purchases.items{ name price }"));
        personList.forEach(person -> person.getPurchases().forEach(purchase -> purchase.getItems().forEach(item ->
                assertTrue(Objects.nonNull(item.getName()) && Objects.nonNull(item.getPrice())
                        && item.getId() == null ))));
    }

    @Test
    public void viewAccount() throws IOException {
        Account account = jackson5.convert(people, Account.class, Fields.Root("[0].account"));
        assertTrue(Objects.nonNull(account.getUsername()));
    }

    @Test
    public void nameAndAddress() throws IOException {
        Person person = jackson5.convert(people
                , Person.class
                , Fields.Root("[0]").include("{ firstName primaryAddress }"));
        assertTrue(Objects.nonNull(person.getFirstName()) && Objects.nonNull(person.getPrimaryAddress()));
    }

    @Test
    public void noNameAndAddress() throws IOException {
        Person person = jackson5.convert(people
                , Person.class
                , Fields.Root("[0]").exclude("{ firstName primaryAddress addresses[0:] }"));
        assertTrue(Objects.nonNull(person.getLastName())
                && person.getPrimaryAddress() == null
                && person.getAddresses().isEmpty() );
    }

    @Test
    public void lastItem() throws IOException {
        Person p = people.get(people.size() - 1);
        Purchase purchase = p.getPurchases().get(p.getPurchases().size() - 1);
        Item item = purchase.getItems().get(purchase.getItems().size() - 1);

        Person person = jackson5.convert(people
                , Person.class
                , Fields.Root("[-1]").include("{ purchases[-1].items[-1] }"));
        assertEquals(item.getName(), person.getPurchases().get(0).getItems().get(0).getName());
    }

    @Test
    public void secondAndFourthLogins() throws IOException {
        List<String> logins = people.get(0).getAccount().getLogins().subList(1, 4);
        Account account = jackson5.convert(people, Account.class, Fields.Root("[0].account").include("logins[1:3]"));
        assertEquals(logins, account.getLogins());
    }

    @Test
    public void excludeSecondAndFourthLogins() throws IOException {
        List<String> currentLogins = people.get(0).getAccount().getLogins();
        List<String> logins = new ArrayList<>();
        logins.add(currentLogins.get(0));
        for (int i = 4; i < currentLogins.size(); i++)
            logins.add(currentLogins.get(i));
        Account account = jackson5.convert(people, Account.class, Fields.Root("[0].account").exclude("logins[1:3]"));
        assertEquals(logins, account.getLogins());
    }

    @Test
    public void vehiclesWithNoVin() throws IOException {
       List<Person> personList = jackson5.convertCollection(people
               , Person.class
               , Fields.Include("lastName vehicles").exclude("vehicles.vin"));
       personList.forEach(p -> {
           assertNull(p.getFirstName());
           assertNotNull(p.getLastName());
           assertFalse(p.getVehicles().isEmpty());
           p.getVehicles().forEach(vehicle -> {
               assertNull(vehicle.getVin());
               assertNotNull(vehicle.getMake());
           });
       });
    }

    @Test
    public void serializeAndDeserializeDates() throws IOException {
        PojoDate pojoDate = new PojoDate();
        pojoDate.setLocalDateTime(LocalDateTime.now());
        pojoDate.setLocalDate(LocalDate.now());
        Date date = new Date();
        pojoDate.setDate(date);
        pojoDate.setSqlDate(new java.sql.Date(date.getTime()));

        String json = jackson5.serialize(pojoDate);
        PojoDate pojoDate1 = jackson5.deserialize(json, PojoDate.class);

        assertEquals(jackson5.serialize(pojoDate.getDate()), jackson5.serialize(pojoDate1.getDate()));
    }

    @Test
    public void defaultDateFormats() throws JsonProcessingException {
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
    public void customDateFormats() throws JsonProcessingException {
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
}
