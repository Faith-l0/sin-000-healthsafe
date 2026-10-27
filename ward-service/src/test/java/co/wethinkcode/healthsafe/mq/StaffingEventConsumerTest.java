package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class StaffingEventConsumerTest {

    @Test
    @DisplayName("should receive broadcast event when topic receives message")
    void testConsumeTopicEvent() throws Exception {
        String testTopicName = "test-staffing-topic-" + System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        StaffingEventConsumer consumer = new StaffingEventConsumer(
                MqConfig.BROKER_URL,
                testTopicName,
                msg -> {
                    receivedMessage.set(msg);
                    latch.countDown();
                }
        );
        consumer.start();

        // Publish to topic
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(testTopicName);
        MessageProducer producer = session.createProducer(topic);

        TextMessage message = session.createTextMessage("{\"event\":\"STAFF_SHORTAGE\",\"wardId\":\"W-02\"}");
        producer.send(message);

        boolean reached = latch.await(4, TimeUnit.SECONDS);
        assertThat(reached).isTrue();
        assertThat(receivedMessage.get()).contains("STAFF_SHORTAGE");
        assertThat(receivedMessage.get()).contains("W-02");

        consumer.close();
        producer.close();
        session.close();
        connection.close();
    }
}
