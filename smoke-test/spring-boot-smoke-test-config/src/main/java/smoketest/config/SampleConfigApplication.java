/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FromEnvConfigurationProperties.class)
public class SampleConfigApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleConfigApplication.class, args);
	}

}
