/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.SpringBootContextLoader;

class ApplicationStartupSpringBootContextLoader extends SpringBootContextLoader {

	@Override
	protected SpringApplication getSpringApplication() {
		SpringApplication application = new SpringApplication();
		application.setApplicationStartup(new BufferingApplicationStartup(1024));
		return application;
	}

}
