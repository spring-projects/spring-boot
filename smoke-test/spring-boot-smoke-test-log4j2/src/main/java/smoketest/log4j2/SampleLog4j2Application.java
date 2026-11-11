/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.log4j2;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleLog4j2Application {

	private static final Logger logger = LoggerFactory.getLogger(SampleLog4j2Application.class);

	@PostConstruct
	public void logSomething() {
		logger.debug("Sample Debug Message");
		logger.trace("Sample Trace Message");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleLog4j2Application.class, args).close();
	}

}
