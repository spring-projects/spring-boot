package sample.activemq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Queue;

@Component
public class Producer {

	@Autowired
	private JmsMessagingTemplate jmsMessagingTemplate;

	@Autowired
	private Queue queue;


	public void sendToQueue() {
		jmsMessagingTemplate.convertAndSend(queue, "Hi Queue!!!");
		System.out.println("Message was sent to the Queue");
	}

}
