/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.staticcontent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SampleWebStaticApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SampleWebStaticApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleWebStaticApplication.class, args);
	}

}
