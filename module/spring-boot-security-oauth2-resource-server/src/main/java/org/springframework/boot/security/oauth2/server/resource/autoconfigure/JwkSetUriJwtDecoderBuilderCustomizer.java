/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder;

/**
 * Callback interface for the customization of the {@link JwkSetUriJwtDecoderBuilder} used
 * to create the auto-configured {@link JwtDecoder} for a JWK set URI that has been
 * configured directly or obtained through an issuer URI.
 *
 * @author Andy Wilkinson
 * @since 4.1.0
 */
@FunctionalInterface
public interface JwkSetUriJwtDecoderBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the {@code builder} to customize
	 */
	void customize(JwkSetUriJwtDecoderBuilder builder);

}
