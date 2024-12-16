package com.example.ktable;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

public class KTableExample {
    static void runKafkaStreams(final KafkaStreams streams) {
        final CountDownLatch latch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (oldState == KafkaStreams.State.RUNNING && newState != KafkaStreams.State.RUNNING) {
                latch.countDown();
            }
        });

        streams.start();

        try {
            latch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Streams Closed");
    }
    static Topology buildTopology(Properties allProps) {
        final StreamsBuilder builder = new StreamsBuilder();
        final String inputTopic = allProps.getProperty("basic.input.topic");
        final String streamsOutputTopic = allProps.getProperty("basic.output.topic");
        final String tableOutputTopic = allProps.getProperty("ktable.example.output.topic");

        final Serde<String> stringSerde = Serdes.String();

        final KStream<String, String> stream = builder.stream(inputTopic, Consumed.with(stringSerde, stringSerde)).peek((k,v) -> System.out.println("Observed event: " + v));

        final KTable<String, String> convertedTable = stream.toTable(Materialized.as("stream-example-converted-to-table"));

        stream.to(streamsOutputTopic, Produced.with(stringSerde, stringSerde));
        convertedTable
        .filter((key, value) -> value.contains("orderNumber-"))
        .mapValues(value -> value.substring(value.indexOf("-") + 1))
        .filter((key, value) -> Long.parseLong(value) > 1000)
        .toStream()
        .peek((key, value) -> System.out.println("Outgoing record - key " + key + " value " + value))
        .to(tableOutputTopic, Produced.with(stringSerde, stringSerde));


        return builder.build();
    }
    public static void main(String[] args) throws Exception {


        final Properties allProps = new Properties();
        try (InputStream inputStream = new FileInputStream("/home/docitu/Documents/data_engineering/ktable/src/main/resources/streams.properties")) {
            allProps.load(inputStream);
        }
        allProps.put(StreamsConfig.APPLICATION_ID_CONFIG, allProps.getProperty("application.id"));
        allProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        allProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final Topology topology = App.buildTopology(allProps);

        final KafkaStreams streams = new KafkaStreams(topology, allProps);
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch Control-C.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });

        try {
            streams.cleanUp();
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        streams.close();
        System.exit(0);
    }
}
