/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.jsp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SampleTomcatJspApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SampleTomcatJspApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleTomcatJspApplication.class, args);
	}

}
