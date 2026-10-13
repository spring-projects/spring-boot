/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.amqp;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleAmqpApplication {

	@Bean
	public ApplicationRunner runner(Sender sender) {
		return (args) -> sender.send("Hello");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleAmqpApplication.class, args);
	}

}
