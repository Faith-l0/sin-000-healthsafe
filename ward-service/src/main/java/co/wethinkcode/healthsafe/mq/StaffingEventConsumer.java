package co.wethinkcode.healthsafe.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.function.Consumer;

public class StaffingEventConsumer implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final MessageConsumer consumer;

    public StaffingEventConsumer(String brokerUrl, String topicName, Consumer<String> onMessageReceived) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        this.connection = factory.createConnection();
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);
        this.consumer = session.createConsumer(topic);

        this.consumer.setMessageListener(message -> {
            if (message instanceof TextMessage textMessage) {
                try {
                    onMessageReceived.accept(textMessage.getText());
                } catch (JMSException e) {
                    System.err.println("Error processing JMS message: " + e.getMessage());
                }
            }
        });
    }

    public void start() throws JMSException {
        this.connection.start();
    }

    @Override
    public void close() throws Exception {
        if (consumer != null) consumer.close();
        if (session != null) session.close();
        if (connection != null) connection.close();
    }
}
