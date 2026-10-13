/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.flyway;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleFlywayApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleFlywayApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(PersonRepository repository) {
		return (args) -> System.err.println(repository.findAll());
	}

}
