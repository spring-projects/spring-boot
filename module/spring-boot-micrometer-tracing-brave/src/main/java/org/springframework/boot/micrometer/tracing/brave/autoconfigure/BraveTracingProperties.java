/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.brave.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for tracing with Brave.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.brave.tracing")
public class BraveTracingProperties {

	/**
	 * Whether the propagation type and tracing backend support sharing the span ID
	 * between client and server spans. Requires B3 propagation and a compatible backend.
	 */
	private boolean spanJoiningSupported;

	public boolean isSpanJoiningSupported() {
		return this.spanJoiningSupported;
	}

	public void setSpanJoiningSupported(boolean spanJoiningSupported) {
		this.spanJoiningSupported = spanJoiningSupported;
	}

}
