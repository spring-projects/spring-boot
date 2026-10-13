/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleSessionRedisApplication.class);
	}

}
