/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.propertyvalidation;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("sample")
@Validated
public class SampleProperties {

	/**
	 * Sample host.
	 */
	private @Nullable String host;

	/**
	 * Sample port.
	 */
	private Integer port = 8080;

	public @Nullable String getHost() {
		return this.host;
	}

	public void setHost(@Nullable String host) {
		this.host = host;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

}
