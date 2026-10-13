/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.reactive;

import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;

/**
 * Customizer that can be used to modify the auto-configured
 * {@link ClientHttpConnectorBuilder} when its type matches.
 *
 * @param <B> the builder type
 * @author Phillip Webb
 * @since 3.5.0
 */
public interface ClientHttpConnectorBuilderCustomizer<B extends ClientHttpConnectorBuilder<?>> {

	/**
	 * Customize the given builder.
	 * @param builder the builder to customize
	 * @return the customized builder
	 */
	B customize(B builder);

}
