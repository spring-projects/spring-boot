/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Jersey application that uses Jackson 2.
 *
 * @author Kristoffer Larsen Hopland
 */
@SpringBootApplication
public class SampleJerseyJackson2Application {

	public static void main(String[] args) {
		SpringApplication.run(SampleJerseyJackson2Application.class, args);
	}

}
