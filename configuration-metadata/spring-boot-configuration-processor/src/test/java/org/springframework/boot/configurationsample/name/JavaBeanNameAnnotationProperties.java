/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.name;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestName;

/**
 * Java bean properties making use of {@code @Name}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("named")
public class JavaBeanNameAnnotationProperties {

	/**
	 * Imports to apply.
	 */
	@TestName("import")
	private String imports;

	/**
	 * Whether default mode is enabled.
	 */
	@TestName("default")
	private boolean defaultValue = true;

	public String getImports() {
		return this.imports;
	}

	public void setImports(String imports) {
		this.imports = imports;
	}

	public boolean isDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

}
