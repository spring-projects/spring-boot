/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.rabbitmq;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Payload;

@SpringBootApplication
@RabbitListener(queues = "foo")
public class SampleRabbitApplication {

	private static final Log logger = LogFactory.getLog(SampleRabbitApplication.class);

	@Bean
	public Sender mySender() {
		return new Sender();
	}

	@Bean
	public Queue fooQueue() {
		return new Queue("foo");
	}

	@RabbitHandler
	public void process(@Payload String foo) {
		logger.info(foo);
	}

	@Bean
	public ApplicationRunner runner(Sender sender) {
		return (args) -> sender.send("Hello");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleRabbitApplication.class, args);
	}

}
