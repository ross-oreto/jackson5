package io.oreto.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.oreto.jackson.pojos.Pojo1;
import io.oreto.jackson.pojos.Pojo2;
import io.oreto.jackson.util.TestUtils;
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
    static List<Pojo1> pojos = new ArrayList<>();
    static List<Pojo2> pojos2 = new ArrayList<>();
    static String jsonString;

    static  {
        for(int i = 0; i < 1000; i++) {
            pojos.add(new Pojo1(TestUtils.randomString(20).toString(), TestUtils.randomString(20).toString(), TestUtils.randomString(20).toString()));
            pojos2.add(new Pojo2(TestUtils.randomString(20).toString(), TestUtils.randomString(20).toString(), TestUtils.randomString(20).toString()));
        }
        try {
            jsonString = jackson5.serialize(pojos2);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
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
        blackhole.consume(jackson5.deserializeCollection(jsonString, Pojo2.class, Fields.Exclude("s2 s3")));
    }
}
