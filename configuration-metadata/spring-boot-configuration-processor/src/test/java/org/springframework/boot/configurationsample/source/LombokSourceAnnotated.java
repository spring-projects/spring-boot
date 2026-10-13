/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

import lombok.Getter;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@Getter
@TestConfigurationProperties("example")
public class LombokSourceAnnotated {

	@TestNestedConfigurationProperty
	private final LombokSource nested = new LombokSource();

}
