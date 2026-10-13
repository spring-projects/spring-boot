/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.name;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestName;

/**
 * Sample where two constructor parameters use the same {@code @Name} value (invalid).
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("duplicate")
public class DuplicateNameConstructorParameterProperties {

	private final String company;

	private final String name;

	public DuplicateNameConstructorParameterProperties(@TestName("aaa") String company, @TestName("aaa") String name) {
		this.company = company;
		this.name = name;
	}

	public String getCompany() {
		return this.company;
	}

	public String getName() {
		return this.name;
	}

}
