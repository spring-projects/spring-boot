/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.noweb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service", ignoreUnknownFields = false)
public class ServiceProperties {

	/**
	 * Name of the service.
	 */
	private String name = "World";

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
