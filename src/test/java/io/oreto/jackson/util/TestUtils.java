package io.oreto.jackson.util;

import io.oreto.jackson.models.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {
    public static Random random = new Random();
    public static LocalDateTime NOW = LocalDateTime.now();
    static long ID = 1L;

    public static String randomString(int size) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    public static Long id() {
        return ID + 1;
    }

    public static Person randomPerson() {
        return new Person()
                .withId(id())
                .withFirstName(randomString(10))
                .withLastName(randomString(10))
                .withAddresses(randomAddresses(1 + random.nextInt(2)))
                .withPrimaryAddress(randomAddress())
                .withVehicles(randomVehicles(1 + random.nextInt(2)))
                .withPurchases(randomPurchases(random.nextInt(50)))
                .withAccount(randomAccount());
    }
    public static List<Person> randomPeople(int n) {
        return IntStream.range(0, n).mapToObj(i -> randomPerson()).collect(Collectors.toList());
    }

    public static Address randomAddress() {
        return new Address()
                .withId(id())
                .withLine1(randomString(10))
                .withLine2(randomString(10))
                .withCity(randomString(10))
                .withState(randomString(2))
                .withZip(random.nextInt(5));
    }
    public static List<Address> randomAddresses(int n) {
        return IntStream.range(0, n).mapToObj(i -> randomAddress()).collect(Collectors.toList());
    }

    public static Purchase randomPurchase() {
        return new Purchase()
                .withId(id())
                .withPurchasedOn(NOW.minusDays(random.nextInt(30)))
                .withItems(randomItems(random.nextInt(200)));
    }
    public static List<Purchase> randomPurchases(int n) {
        return IntStream.range(0, n).mapToObj(i -> randomPurchase()).collect(Collectors.toList());
    }

    public static Item randomItem() {
        return new Item()
                .withId(id())
                .withName(randomString(20))
                .withPrice(randomPrice());
    }
    public static List<Item> randomItems(int n) {
        return IntStream.range(0, n).mapToObj(i -> randomItem()).collect(Collectors.toList());
    }
    public static BigDecimal randomPrice() {
        return BigDecimal.valueOf(random.nextInt(10000) + random.nextDouble())
                .setScale(2, RoundingMode.CEILING);
    }

    public static Vehicle randomVehicle() {
        return new Vehicle()
                .withVin(randomString(17))
                .withMake(randomString(20))
                .withModel(randomString(20))
                .withYear(NOW.minusYears(random.nextInt(10)).getYear());
    }
    public static List<Vehicle> randomVehicles(int n) {
        return IntStream.range(0, n).mapToObj(i -> randomVehicle()).collect(Collectors.toList());
    }

    public static Account randomAccount() {
        return new Account()
                .withUsername(randomString(10))
                .withEmail(randomString(20)).withLogins(logins(10));
    }
    public static List<String> logins(int n) {
        return IntStream.range(0, n).mapToObj(i -> randomString(10)).collect(Collectors.toList());
    }
}
