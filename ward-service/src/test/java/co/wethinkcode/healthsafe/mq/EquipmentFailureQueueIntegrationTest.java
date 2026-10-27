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

public class EquipmentFailureQueueIntegrationTest {

    @Test
    @DisplayName("should consume failure message from queue and verify queue drain")
    void testQueueMessageDeliveryAndDrain() throws Exception {
        String testQueue = "test-drain-queue-" + System.currentTimeMillis();

        EquipmentFailureProducer producer = new EquipmentFailureProducer(MqConfig.BROKER_URL, testQueue);
        producer.sendFailureAlert("W-03", "Defibrillator", "Battery depletion");

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(testQueue);
        MessageConsumer consumer = session.createConsumer(queue);

        // Receive message
        TextMessage msg = (TextMessage) consumer.receive(3000);
        assertThat(msg).isNotNull();
        assertThat(msg.getText()).contains("Defibrillator");

        // Next receive should be empty because queue message was acknowledged and consumed
        TextMessage emptyMsg = (TextMessage) consumer.receive(1000);
        assertThat(emptyMsg).isNull();

        producer.close();
        consumer.close();
        session.close();
        connection.close();
    }
}
