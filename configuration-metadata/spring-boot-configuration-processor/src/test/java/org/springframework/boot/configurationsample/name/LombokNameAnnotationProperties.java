/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.name;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestName;

/**
 * Lombok properties making use of {@code @Name}.
 *
 * @author Stephane Nicoll
 */
@Getter
@Setter
@TestConfigurationProperties("named")
public class LombokNameAnnotationProperties {

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

}
