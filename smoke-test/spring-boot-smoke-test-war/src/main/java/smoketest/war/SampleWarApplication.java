/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.war;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("WEB-INF/custom.properties")
public class SampleWarApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SampleWarApplication.class, args);
	}

}
