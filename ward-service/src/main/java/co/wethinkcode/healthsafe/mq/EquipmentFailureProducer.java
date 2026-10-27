package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class EquipmentFailureProducer implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public EquipmentFailureProducer(String brokerUrl, String queueName) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        this.connection = factory.createConnection();
        this.connection.start();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(queueName);
        this.producer = session.createProducer(queue);
        this.producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    }

    public synchronized void sendFailureAlert(String wardId, String equipmentType, String reason) throws JMSException {
        String jsonPayload = String.format(
                "{\"wardId\":\"%s\",\"equipmentType\":\"%s\",\"reason\":\"%s\",\"timestamp\":%d}",
                wardId, equipmentType, reason, System.currentTimeMillis()
        );
        TextMessage textMessage = session.createTextMessage(jsonPayload);
        producer.send(textMessage);
    }

    @Override
    public void close() throws Exception {
        if (producer != null) producer.close();
        if (session != null) session.close();
        if (connection != null) connection.close();
    }
}
