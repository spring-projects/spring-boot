/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.logback;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleLogbackApplication {

	private static final Logger logger = LoggerFactory.getLogger(SampleLogbackApplication.class);

	@PostConstruct
	public void logSomething() {
		logger.debug("Sample Debug Message");
		logger.trace("Sample Trace Message");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleLogbackApplication.class, args).close();
	}

}
