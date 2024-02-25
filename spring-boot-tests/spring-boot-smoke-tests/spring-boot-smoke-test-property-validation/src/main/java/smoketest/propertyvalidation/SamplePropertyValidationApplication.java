/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.propertyvalidation;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.Validator;

/**
 * SamplePropertyValidationApplication class.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SamplePropertyValidationApplication implements CommandLineRunner {

	private final SampleProperties properties;

	/**
	 * Constructs a new SamplePropertyValidationApplication with the specified
	 * SampleProperties.
	 * @param properties the SampleProperties to be used by the application
	 */
	public SamplePropertyValidationApplication(SampleProperties properties) {
		this.properties = properties;
	}

	/**
	 * Returns a new instance of {@link SamplePropertiesValidator} that implements the
	 * {@link Validator} interface. This method is annotated with {@link Bean} to indicate
	 * that it is a Spring bean definition.
	 * @return a new instance of {@link SamplePropertiesValidator}
	 */
	@Bean
	public static Validator configurationPropertiesValidator() {
		return new SamplePropertiesValidator();
	}

	/**
	 * This method is the entry point of the application and is responsible for running
	 * the application. It prints the sample host and port values from the properties
	 * file.
	 * @param args The command line arguments passed to the application.
	 */
	@Override
	public void run(String... args) {
		System.out.println("=========================================");
		System.out.println("Sample host: " + this.properties.getHost());
		System.out.println("Sample port: " + this.properties.getPort());
		System.out.println("=========================================");
	}

	/**
	 * The main method is the entry point of the application. It initializes and runs the
	 * SpringApplicationBuilder to start the SamplePropertyValidationApplication.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		new SpringApplicationBuilder(SamplePropertyValidationApplication.class).run(args);
	}

}
