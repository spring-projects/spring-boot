/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Example {@link ConstructorBinding constructor-bound}
 * {@link ConfigurationProperties @ConfigurationProperties} used to test the use of
 * configuration properties scan with sliced test.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("example")
public class ExampleProperties {

	private final String name;

	public ExampleProperties(@DefaultValue("test") String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
