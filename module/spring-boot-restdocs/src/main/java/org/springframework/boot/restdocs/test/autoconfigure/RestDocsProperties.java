/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restdocs.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring REST Docs.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @since 4.0.0
 */
@ConfigurationProperties("spring.test.restdocs")
public class RestDocsProperties {

	/**
	 * The URI scheme for to use (for example http).
	 */
	private @Nullable String uriScheme;

	/**
	 * The URI host to use.
	 */
	private @Nullable String uriHost;

	/**
	 * The URI port to use.
	 */
	private @Nullable Integer uriPort;

	public @Nullable String getUriScheme() {
		return this.uriScheme;
	}

	public void setUriScheme(@Nullable String uriScheme) {
		this.uriScheme = uriScheme;
	}

	public @Nullable String getUriHost() {
		return this.uriHost;
	}

	public void setUriHost(@Nullable String uriHost) {
		this.uriHost = uriHost;
	}

	public @Nullable Integer getUriPort() {
		return this.uriPort;
	}

	public void setUriPort(@Nullable Integer uriPort) {
		this.uriPort = uriPort;
	}

}
