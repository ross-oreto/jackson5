package io.oreto.jackson;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Lists {
    @SafeVarargs
    public static <T> List<T> of(T... t) {
        return Arrays.stream(t).collect(Collectors.toList());
    }
}
