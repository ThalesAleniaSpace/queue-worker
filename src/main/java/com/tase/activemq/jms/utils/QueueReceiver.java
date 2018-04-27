package com.tase.activemq.jms.utils;

import java.io.Serializable;
import java.nio.charset.Charset;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQObjectMessage;

public class QueueReceiver {
	// Character set
	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final String NMS_ACTIVEMQ_USERNAME = "NMS_ACTIVEMQ_USERNAME";
	private static final String NMS_ACTIVEMQ_PASSWORD = "NMS_ACTIVEMQ_PASSWORD";
	private static final String NMS_ACTIVEMQ_HOSTNAME = "NMS_ACTIVEMQ_HOSTNAME";
	private static final String NMS_ACTIVEMQ_PORT = "NMS_ACTIVEMQ_PORT";
	private static final String NMS_ACTIVEMQ_URL = "failover://tcp://HOSTNAME:PORT";

	// Message broker user
	private String user = "hack";
	// Message broker password
	private String password = "hack";
	// Message broker password
	private String hostname = "127.0.0.1";
	//private String hostname = "activemq";
	// Message broker password
	private String port = "61616";
	// Connection string
	private String url = null;

	private static QueueReceiver instance = null;

	protected QueueReceiver() {

		// Loads configuration from the environment
		String activeMQUser = System.getenv(NMS_ACTIVEMQ_USERNAME);
		String activeMQPasword = System.getenv(NMS_ACTIVEMQ_PASSWORD);
		String activeMQHostname = System.getenv(NMS_ACTIVEMQ_HOSTNAME);
		String activeMQPort = System.getenv(NMS_ACTIVEMQ_PORT);

		// Replace default configuration if environment variables are present
		user = activeMQUser != null ? activeMQUser : user;
		password = activeMQPasword != null ? activeMQPasword : password;
		hostname = activeMQHostname != null ? activeMQHostname : hostname;
		port = activeMQPort != null ? activeMQPort : port;

		// Create connection string
		url = NMS_ACTIVEMQ_URL.replace("HOSTNAME", hostname).replace("PORT", port);
	}

	public static QueueReceiver getInstance() {
		if (instance == null) {
			synchronized (QueueReceiver.class) {
				if (instance == null) {
					instance = new QueueReceiver();
				}
			}
		}
		return instance;
	}

	public void sendMessage(String message, String queueName, String correlationId) throws JMSException {
		
		Connection connection = null;
		try {

			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
			connection = connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(queueName);

			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			TextMessage textMessage = session.createTextMessage(message);
			textMessage.setJMSCorrelationID(correlationId);
			producer.send(textMessage);
			// ObjectMessage objectMessage =
			// session.createObjectMessage(message);
			// producer.send(objectMessage);
			producer.close();
			session.close();
		} finally {
			
			if (connection != null) {
				connection.stop();
				connection.close();
			}
		}
	}
	
	public String[] receiveMessage(String queueName) throws JMSException {

		Connection connection = null;
		String[] received;
		
		try {
			ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
			connection = connectionFactory.createConnection();
			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(queueName);
			MessageConsumer consumer = session.createConsumer(destination);
			//MessageConsumer consumer = session.createConsumer(destination);
			Message message = consumer.receive();
			
			String correlation = message.getJMSCorrelationID();
			String body = getStringFromMessage(message);
			
			received = new String[]{correlation, body};
			
			consumer.close();
			session.close();
			return received;
			
		} finally {
			if (connection != null) {
				connection.stop();
				connection.close();
			}
		}
		
	}
	
	private String getStringFromMessage(Message message) {
		String messageText = null;
		try {
			// For a text message
			if (message instanceof TextMessage) {
				messageText = ((TextMessage) message).getText();
			}
			// For a byte message
			else if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage) message;

				int length = (int) bytesMessage.getBodyLength();
				byte[] preview = new byte[length];
				bytesMessage.readBytes(preview);
				messageText = new String(preview, CHARSET);
			}
			// For an ActiveMQ message
			else if (message instanceof ActiveMQObjectMessage) {
				messageText = ((ActiveMQObjectMessage) message).getObject().toString();
			}
		} catch (JMSException e) {

		}
		return messageText;
	}
}
