/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.endpoint;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestEndpoint;
import org.springframework.boot.configurationsample.TestReadOperation;

/**
 * An endpoint with additional custom properties.
 *
 * @author Stephane Nicoll
 */
@TestEndpoint(id = "customprops")
@TestConfigurationProperties("management.endpoint.customprops")
public class CustomPropertiesEndpoint {

	private String name = "test";

	@TestReadOperation
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
