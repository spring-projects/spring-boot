/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Sample application to demonstrate testing.
 *
 * @author Phillip Webb
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SampleTestApplication {

	// NOTE: this application will intentionally not start without MySQL, the test will
	// still run.

	public static void main(String[] args) {
		SpringApplication.run(SampleTestApplication.class, args);
	}

}
