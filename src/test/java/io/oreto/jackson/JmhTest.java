package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.oreto.jackson.pojos.JPojo1;
import io.oreto.jackson.pojos.JPojo2;
import io.oreto.jackson.pojos.Pojo1;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JmhTest {
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(JmhTest.class.getName() + ".*")
                .mode(Mode.AverageTime)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(2)
                .threads(1)
                .measurementIterations(3)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        new Runner(options).run();
    }

    static ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    static Jackson5 jackson5 = Jackson5.get();
    static List<JPojo1> pojos = new ArrayList<>();
    static List<JPojo2> pojos2 = new ArrayList<>();
    static String jsonString;

    static  {
        for(int i = 0; i < 1000; i++) {
            pojos.add(new JPojo1(random(20).toString(), random(20).toString(), random(20).toString()));
            pojos2.add(new JPojo2(random(20).toString(), random(20).toString(), random(20).toString()));
        }
        try {
            jsonString = jackson5.serialize(pojos2);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static StringBuilder random(int size) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void anObjectMapperSerialize(Blackhole blackhole) throws JsonProcessingException {
        blackhole.consume(mapper.writer().writeValueAsString(pojos));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void jackson5Serialize(Blackhole blackhole) throws JsonProcessingException {
        blackhole.consume(jackson5.serialize(pojos2, Fields.Exclude("s2 s3")));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void anObjectMapperDeSerialize(Blackhole blackhole) throws JsonProcessingException {
        blackhole.consume(mapper.readValue(jsonString, new TypeReference<List<Pojo1>>(){}));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void jackson5DeSerialize(Blackhole blackhole) throws IOException {
        blackhole.consume(jackson5.deserializeCollection(jsonString, JPojo2.class, Fields.Exclude("s2 s3")));
    }
}
