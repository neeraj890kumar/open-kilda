package org.bitbucket.openkilda.wfm;

import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.serializer.StringDecoder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.bitbucket.openkilda.wfm.TestUtils.kafkaUrl;
import static org.bitbucket.openkilda.wfm.TestUtils.zookeeperUrl;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This class is mostly an example of how to startup zookeeper/kafka and send/receive messages.
 * It should be useful when developing Storm Topologies that have a Kafka Spout
 */
public class SimpleKafkaTest {
    public static final String topic = "simple-kafka"; // + System.currentTimeMillis();

    private TestUtils.KafkaTestFixture server;
    private Producer<String, String> producer;
    private ConsumerConnector consumerConnector;

    @Before
    public void setup() throws Exception {
        server = new TestUtils.KafkaTestFixture();
        server.start(TestUtils.serverProperties());
    }

    @After
    public void teardown() throws Exception {
        producer.close();
        consumerConnector.shutdown();
        server.stop();
    }

    @Test
    public void shouldWriteThenRead() throws Exception {

        //Create a consumer
        ConsumerIterator<String, String> it = buildConsumer(SimpleKafkaTest.topic);

        //Create a producer
        producer = new KafkaProducer<>(producerProps());

        //send a message
        producer.send(new ProducerRecord<>(SimpleKafkaTest.topic, "message")).get();

        //read it back
        MessageAndMetadata<String, String> messageAndMetadata = it.next();
        String value = messageAndMetadata.message();
        assertThat(value, is("message"));
    }

    private ConsumerIterator<String, String> buildConsumer(String topic) {
        Properties props = consumerProperties();

        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topic, 1);
        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        Map<String, List<KafkaStream<String, String>>> consumers = consumerConnector.createMessageStreams(
                topicCountMap, new StringDecoder(null), new StringDecoder(null));
        KafkaStream<String, String> stream = consumers.get(topic).get(0);
        return stream.iterator();
    }

    private Properties consumerProperties() {
        Properties props = new Properties();
        props.put("zookeeper.connect", zookeeperUrl);
        props.put("group.id", "group1");
        props.put("auto.offset.reset", "smallest");
        return props;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaUrl);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("request.required.acks", "1");
        return props;
    }
}
