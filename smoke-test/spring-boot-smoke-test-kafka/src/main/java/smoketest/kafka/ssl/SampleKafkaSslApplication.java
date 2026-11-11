/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.kafka.ssl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleKafkaSslApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleKafkaSslApplication.class, args);
	}

}
