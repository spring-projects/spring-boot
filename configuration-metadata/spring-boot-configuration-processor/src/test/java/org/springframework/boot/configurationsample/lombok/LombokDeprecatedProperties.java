/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Deprecated configuration properties.
 *
 * @author Stephane Nicoll
 * @deprecated deprecated
 */
@Getter
@Setter
@TestConfigurationProperties("deprecated")
@Deprecated
@SuppressWarnings("unused")
public class LombokDeprecatedProperties {

	private String name;

	private String description;

}
