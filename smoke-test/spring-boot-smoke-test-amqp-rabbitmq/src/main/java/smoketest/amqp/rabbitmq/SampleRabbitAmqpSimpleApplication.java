/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.amqp.rabbitmq;

import com.rabbitmq.client.amqp.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleRabbitAmqpSimpleApplication {

	private static final Log logger = LogFactory.getLog(SampleRabbitAmqpSimpleApplication.class);

	@Bean
	public Sender mySender() {
		return new Sender();
	}

	@Bean
	public Queue fooQueue() {
		return new Queue("foo");
	}

	@RabbitListener(queues = "foo")
	public void process(Message amqpMessage) {
		String body = new String(amqpMessage.body());
		String sourceApp = (String) amqpMessage.property("source-app");
		logger.info("Received '%s' from '%s'".formatted(body, sourceApp));
	}

	@Bean
	public ApplicationRunner runner(Sender sender) {
		return (args) -> sender.send("Hello");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleRabbitAmqpSimpleApplication.class, args);
	}

}
