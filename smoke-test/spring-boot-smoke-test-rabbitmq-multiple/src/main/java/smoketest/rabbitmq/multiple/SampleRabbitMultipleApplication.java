/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.rabbitmq.multiple;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleRabbitMultipleApplication {

	@Bean
	public Queue fooQueue() {
		return new Queue("foo");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleRabbitMultipleApplication.class, args);
	}

}
