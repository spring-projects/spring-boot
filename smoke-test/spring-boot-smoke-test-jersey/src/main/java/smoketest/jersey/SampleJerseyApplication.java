/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SampleJerseyApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		new SampleJerseyApplication()
			.configure(new SpringApplicationBuilder(SampleJerseyApplication.class)
				.applicationStartup(new BufferingApplicationStartup(2048)))
			.run(args);
	}

}
