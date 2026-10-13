/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.resttestclient.autoconfigure;

import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.Builder;

/**
 * A customizer that can be implemented by beans wishing to customize the {@link Builder
 * RestTestClient.Builder} to fine-tune its auto-configuration before a
 * {@link RestTestClient} is created.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface RestTestClientBuilderCustomizer {

	/**
	 * Customize the given {@link Builder RestTestClient.Builder}.
	 * @param builder the builder
	 */
	void customize(RestTestClient.Builder<?> builder);

}
