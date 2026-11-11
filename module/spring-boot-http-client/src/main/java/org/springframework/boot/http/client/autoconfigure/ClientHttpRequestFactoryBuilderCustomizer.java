/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;

/**
 * Customizer that can be used to modify the auto-configured
 * {@link ClientHttpRequestFactoryBuilder} when its type matches.
 *
 * @param <B> the builder type
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface ClientHttpRequestFactoryBuilderCustomizer<B extends ClientHttpRequestFactoryBuilder<?>> {

	/**
	 * Customize the given builder.
	 * @param builder the builder to customize
	 * @return the customized builder
	 */
	B customize(B builder);

}
