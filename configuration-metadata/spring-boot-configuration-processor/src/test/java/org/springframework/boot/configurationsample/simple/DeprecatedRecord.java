/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDeprecatedConfigurationProperty;
import org.springframework.boot.configurationsample.TestName;

/**
 * Configuration properties as record with deprecated property.
 *
 * @param alpha alpha property, deprecated
 * @param bravo bravo property
 * @param charlie charlie property, named, deprecated
 * @author Moritz Halbritter
 */
@TestConfigurationProperties("deprecated-record")
public record DeprecatedRecord(String alpha, String bravo, @TestName("named.charlie") String charlie) {

	@Deprecated
	@TestDeprecatedConfigurationProperty(reason = "some-reason")
	public String alpha() {
		return this.alpha;
	}

	@Deprecated
	@TestDeprecatedConfigurationProperty(reason = "another-reason")
	public String charlie() {
		return this.charlie;
	}
}
