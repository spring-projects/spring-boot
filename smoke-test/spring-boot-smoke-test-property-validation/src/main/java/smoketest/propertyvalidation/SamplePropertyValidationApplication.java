/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.propertyvalidation;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SamplePropertyValidationApplication implements CommandLineRunner {

	private final SampleProperties properties;

	public SamplePropertyValidationApplication(SampleProperties properties) {
		this.properties = properties;
	}

	@Bean
	public static Validator configurationPropertiesValidator() {
		return new SamplePropertiesValidator();
	}

	@Override
	public void run(String... args) {
		System.out.println("=========================================");
		System.out.println("Sample host: " + this.properties.getHost());
		System.out.println("Sample port: " + this.properties.getPort());
		System.out.println("=========================================");
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SamplePropertyValidationApplication.class).run(args);
	}

}
