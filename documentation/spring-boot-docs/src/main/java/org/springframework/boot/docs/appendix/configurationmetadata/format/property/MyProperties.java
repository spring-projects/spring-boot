/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.format.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

@ConfigurationProperties("my.app")
public class MyProperties {

	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "my.app.name", since = "1.2.0")
	public String getTarget() {
		return this.name;
	}

	@Deprecated
	public void setTarget(String target) {
		this.name = target;
	}

}
