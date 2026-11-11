/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.bindingnonstatic;

import java.util.List;

import org.springframework.boot.build.architecture.annotations.TestConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;

public class BindingMethodNonStatic {

	@Bean
	@TestConfigurationPropertiesBinding
	public List<String> binder() {
		return List.of("hello", "world");
	}

}
