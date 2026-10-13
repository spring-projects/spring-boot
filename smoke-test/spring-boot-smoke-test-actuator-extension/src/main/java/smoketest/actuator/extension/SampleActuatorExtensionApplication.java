/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.extension;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class SampleActuatorExtensionApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleActuatorExtensionApplication.class, args);
	}

}
