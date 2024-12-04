package com.example.ktable;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

public class Util implements AutoCloseable {
    static Topology buildTopology(String inputTopic, String outputTopic) {
    Serde<String> stringSerde = Serdes.String();
    StreamsBuilder builder = new StreamsBuilder();
    builder
        .stream(inputTopic, Consumed.with(stringSerde, stringSerde))
        .peek((k,v) -> System.out.println("Observed event: " + v))
        .mapValues(s -> s.toUpperCase())
        .peek((k,v) -> System.out.println("Transformed event: {}" + v))
        .to(outputTopic, Produced.with(stringSerde, stringSerde));
    return builder.build();
}

    @Override
    public void close() throws Exception {
        
    }

}
