package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class EquipmentFailureProducerTest {

    @Test
    @DisplayName("should serialize and publish failure alert to equipment-failure-queue")
    void testPublishEquipmentFailure() throws Exception {
        String testQueueName = "test-equipment-failure-queue-" + System.currentTimeMillis();
        EquipmentFailureProducer producer = new EquipmentFailureProducer(MqConfig.BROKER_URL, testQueueName);

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(testQueueName);
        MessageConsumer consumer = session.createConsumer(queue);

        producer.sendFailureAlert("W-01", "Ventilator", "Pressure sensor malfunction");

        TextMessage received = (TextMessage) consumer.receive(3000);
        assertThat(received).isNotNull();
        assertThat(received.getText()).contains("W-01");
        assertThat(received.getText()).contains("Ventilator");
        assertThat(received.getText()).contains("Pressure sensor malfunction");

        producer.close();
        consumer.close();
        session.close();
        connection.close();
    }
}
