/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.Data;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties with a single deprecated element.
 *
 * @author Stephane Nicoll
 */
@Data
@TestConfigurationProperties("singledeprecated")
@SuppressWarnings("unused")
public class LombokDeprecatedSingleProperty {

	@Deprecated
	private String name;

	private String description;

}
