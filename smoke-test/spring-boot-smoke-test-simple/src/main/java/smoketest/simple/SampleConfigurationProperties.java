/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.simple;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("sample")
public final class SampleConfigurationProperties {

	@NotNull
	@SuppressWarnings("NullAway.Init")
	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
