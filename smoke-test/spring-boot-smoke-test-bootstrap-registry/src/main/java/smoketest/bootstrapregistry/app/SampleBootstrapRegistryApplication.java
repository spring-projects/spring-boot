/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.bootstrapregistry.app;

import smoketest.bootstrapregistry.external.svn.SubversionBootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleBootstrapRegistryApplication {

	public static void main(String[] args) {
		// This example shows how a Bootstrapper can be used to register a custom
		// SubversionClient that still has access to data provided in the
		// application.properties file
		SpringApplication application = new SpringApplication(SampleBootstrapRegistryApplication.class);
		application.addBootstrapRegistryInitializer(SubversionBootstrap.withCustomClient(MySubversionClient::new));
		application.run(args);
	}

}
