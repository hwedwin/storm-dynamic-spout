package com.salesforce.storm.spout.sideline;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.salesforce.storm.spout.sideline.config.SidelineSpoutConfig;
import com.salesforce.storm.spout.sideline.filter.StaticMessageFilter;
import com.salesforce.storm.spout.sideline.kafka.SidelineConsumerTest;
import com.salesforce.storm.spout.sideline.kafka.KafkaTestServer;
import com.salesforce.storm.spout.sideline.mocks.MockTopologyContext;
import com.salesforce.storm.spout.sideline.mocks.output.MockSpoutOutputCollector;
import com.salesforce.storm.spout.sideline.trigger.StartRequest;
import com.salesforce.storm.spout.sideline.trigger.StaticTrigger;
import com.salesforce.storm.spout.sideline.trigger.StopRequest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.storm.shade.com.google.common.base.Charsets;
import org.apache.storm.task.TopologyContext;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 *
 */
public class SidelineSpoutTest {

    private static final Logger logger = LoggerFactory.getLogger(SidelineSpoutTest.class);
    private KafkaTestServer kafkaTestServer;
    private String topicName;

    /**
     * Here we stand up an internal test kafka and zookeeper service.
     */
    @Before
    public void setup() throws Exception {
        // ensure we're in a clean state
        tearDown();

        // Setup kafka test server
        kafkaTestServer = new KafkaTestServer();
        kafkaTestServer.start();

        // Generate topic name
        topicName = SidelineConsumerTest.class.getSimpleName() + DateTime.now().getMillis();

        // Create topic
        kafkaTestServer.createTopic(topicName);
    }

    /**
     * Here we shut down the internal test kafka and zookeeper services.
     */
    @After
    public void tearDown() {
        // Close out kafka test server if needed
        if (kafkaTestServer == null) {
            return;
        }
        try {
            kafkaTestServer.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        kafkaTestServer = null;
    }

    /**
     * Simple end-2-end test.  Likely to change drastically as we make further progress.
     */
    @Test
    public void doTest() throws InterruptedException {
        // Define how many tuples we should push into the topic, and then consume back out.
        final int emitTupleCount = 10;

        // Mock Config
        final Map<String, Object> config = Maps.newHashMap();
        config.put(SidelineSpoutConfig.KAFKA_TOPIC, topicName);
        config.put(SidelineSpoutConfig.CONSUMER_ID_PREFIX, "SidelineSpout-");
        config.put(SidelineSpoutConfig.KAFKA_BROKERS, Lists.newArrayList("localhost:" + kafkaTestServer.getKafkaServer().serverConfig().advertisedPort()));

        // Some mock stuff to get going
        final TopologyContext topologyContext = new MockTopologyContext();
        final MockSpoutOutputCollector spoutOutputCollector = new MockSpoutOutputCollector();

        final StaticTrigger staticTrigger = new StaticTrigger();

        // Create spout and call open
        final SidelineSpout spout = new SidelineSpout();
        spout.setStartingTrigger(staticTrigger);
        spout.setStoppingTrigger(staticTrigger);
        spout.open(config, topologyContext, spoutOutputCollector);

        final StaticMessageFilter staticMessageFilter = new StaticMessageFilter();

        // Begin sidelining account 1
        staticTrigger.sendStartRequest(
            new StartRequest(
                Lists.newArrayList(
                    staticMessageFilter
                )
            )
        );

        // Account 1 should not be sidelined

        // Call next tuple, topic is empty, so should get nothing.
        spout.nextTuple();
        assertTrue("SpoutOutputCollector should have no emissions", spoutOutputCollector.getEmissions().isEmpty());

        // Lets produce some data into the topic
        produceRecords(emitTupleCount);

        // Wait a bit
        Thread.sleep(3000);

        // Now loop and get our tuples
        for (int x=0; x<emitTupleCount; x++) {
            // Now ask for next tuple
            spout.nextTuple();

            // Should have some emissions
            assertEquals("SpoutOutputCollector should have emissions", (x + 1), spoutOutputCollector.getEmissions().size());

            // TODO: this sleep should be able to be removed.
            Thread.sleep(500);
        }
        logger.info("Emissions: {}", spoutOutputCollector.getEmissions());

        // Call next tuple a few more times
        for (int x=0; x<3; x++) {
            // This shouldn't get any more tuples
            spout.nextTuple();

            // Should have some emissions
            assertEquals("SpoutOutputCollector should have same number of emissions", emitTupleCount, spoutOutputCollector.getEmissions().size());
        }

        // Stop sidelining account 1
        staticTrigger.sendStopRequest(
            new StopRequest(
                staticTrigger.getCurrentSidelineIdentifier()
            )
        );
        // Everything should be flowing again, no sidelines

        // TODO: Validate account 1 tuples are not processed, and that new ones go through

        // Cleanup.
        spout.close();
    }

    private List<ProducerRecord<byte[], byte[]>> produceRecords(int numberOfRecords) {
        List<ProducerRecord<byte[], byte[]>> producedRecords = Lists.newArrayList();

        KafkaProducer producer = kafkaTestServer.getKafkaProducer("org.apache.kafka.common.serialization.ByteArraySerializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        for (int x=0; x<numberOfRecords; x++) {
            // Construct key and value
            long timeStamp = DateTime.now().getMillis();
            String key = "key" + timeStamp;
            String value = "value" + timeStamp;

            // Construct filter
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topicName, key.getBytes(Charsets.UTF_8), value.getBytes(Charsets.UTF_8));
            producedRecords.add(record);

            // Send it.
            producer.send(record);
        }
        // Publish to the topic and close.
        producer.flush();
        logger.info("Produce completed");
        producer.close();

        return producedRecords;
    }
}