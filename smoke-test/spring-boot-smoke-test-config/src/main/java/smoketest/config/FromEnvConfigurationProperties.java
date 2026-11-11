/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("from-env")
class FromEnvConfigurationProperties {

	private @Nullable String key1;

	@Nullable String getKey1() {
		return this.key1;
	}

	void setKey1(@Nullable String key1) {
		this.key1 = key1;
	}

}
