/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.appendix.configurationmetadata.manualhints.valuehint;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("my")
public class MyProperties {

	private Map<String, Integer> contexts;

	// @fold:on // getters/setters ...
	public Map<String, Integer> getContexts() {
		return this.contexts;
	}

	public void setContexts(Map<String, Integer> contexts) {
		this.contexts = contexts;
	}
	// @fold:off

}
