/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.name;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestName;

/**
 * Sample where two JavaBean properties use the same {@code @Name} value (invalid).
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("duplicate")
public class DuplicateNameJavaBeanProperties {

	@TestName("aaa")
	private String company;

	@TestName("aaa")
	private String name;

	public String getCompany() {
		return this.company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
