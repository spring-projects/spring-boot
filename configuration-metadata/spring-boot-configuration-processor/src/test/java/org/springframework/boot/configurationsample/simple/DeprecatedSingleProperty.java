/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDeprecatedConfigurationProperty;

/**
 * Configuration properties with a single deprecated element.
 *
 * @author Phillip Webb
 */
@TestConfigurationProperties("singledeprecated")
public class DeprecatedSingleProperty {

	private String newName;

	@Deprecated
	@TestDeprecatedConfigurationProperty(reason = "renamed", replacement = "singledeprecated.new-name", since = "1.2.3")
	public String getName() {
		return getNewName();
	}

	@Deprecated
	public void setName(String name) {
		setNewName(name);
	}

	public String getNewName() {
		return this.newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

}
