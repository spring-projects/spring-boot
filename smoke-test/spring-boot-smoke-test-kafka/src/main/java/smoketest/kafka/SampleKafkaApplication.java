/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.kafka;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleKafkaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleKafkaApplication.class, args);
	}

	@Bean
	public NewTopic kafkaTestTopic() {
		return new NewTopic("testTopic", 10, (short) 2);
	}

	@Bean
	public ApplicationRunner runner(Producer producer) {
		return (args) -> producer.send(new SampleMessage(1, "A simple test message"));
	}

}
