/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webtestclient.autoconfigure;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.Builder;

/**
 * A customizer that can be implemented by beans wishing to customize the {@link Builder
 * WebTestClient.Builder} to fine-tune its auto-configuration before a
 * {@link WebTestClient} is created.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface WebTestClientBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder
	 */
	void customize(WebTestClient.Builder builder);

}
