/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.profile;

import smoketest.profile.service.MessageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class SampleProfileApplication implements CommandLineRunner {

	// Simple example shows how a command line spring application can execute an
	// injected bean service. Also demonstrates how you can use @Value to inject
	// command line args ('--test.name=whatever') or application properties

	@Autowired
	private MessageService helloWorldService;

	@Override
	public void run(String... args) {
		System.out.println(this.helloWorldService.getMessage());
	}

	public static void main(String... args) {
		SpringApplication application = new SpringApplication(SampleProfileApplication.class) {

			@Override
			protected void bindToSpringApplication(ConfigurableEnvironment environment) {
			}

		};
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}

}
