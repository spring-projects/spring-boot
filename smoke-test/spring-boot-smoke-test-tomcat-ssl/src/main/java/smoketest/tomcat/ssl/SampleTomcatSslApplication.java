/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.ssl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleTomcatSslApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleTomcatSslApplication.class, args);
	}

}
