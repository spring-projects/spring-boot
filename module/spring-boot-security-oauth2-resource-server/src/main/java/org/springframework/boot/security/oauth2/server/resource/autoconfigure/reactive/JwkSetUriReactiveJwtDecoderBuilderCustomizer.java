/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.JwkSetUriReactiveJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Callback interface for the customization of the
 * {@link JwkSetUriReactiveJwtDecoderBuilder} used to create the auto-configured
 * {@link ReactiveJwtDecoder} for a JWK set URI that has been configured directly or
 * obtained through an issuer URI.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface JwkSetUriReactiveJwtDecoderBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the {@code builder} to customize
	 */
	void customize(JwkSetUriReactiveJwtDecoderBuilder builder);

}
