/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.name;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDefaultValue;
import org.springframework.boot.configurationsample.TestName;

/**
 * Constructor properties making use of {@code @Name}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("named")
public class ConstructorParameterNameAnnotationProperties {

	/**
	 * Imports to apply.
	 */
	private final String imports;

	/**
	 * Whether default mode is enabled.
	 */
	private final boolean defaultValue;

	public ConstructorParameterNameAnnotationProperties(@TestName("import") String imports,
			@TestName("default") @TestDefaultValue("true") boolean defaultValue) {
		this.imports = imports;
		this.defaultValue = defaultValue;
	}

	public String getImports() {
		return this.imports;
	}

	public boolean isDefaultValue() {
		return this.defaultValue;
	}

}
