/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.Data;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties with default values.
 *
 * @author Stephane Nicoll
 */
@Data
@TestConfigurationProperties("default")
@SuppressWarnings("unused")
public class LombokDefaultValueProperties {

	private String description = "my description";

}
