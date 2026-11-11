/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder;

/**
 * Callback interface for the customization of the {@link JwkSetUriJwtDecoderBuilder} used
 * to create the auto-configured {@link JwtDecoder} for a JWK set URI that has been
 * configured directly or obtained through an issuer URI.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
 * {@link org.springframework.boot.security.oauth2.server.resource.autoconfigure.JwkSetUriJwtDecoderBuilderCustomizer}
 */
@FunctionalInterface
@Deprecated(since = "4.1.0", forRemoval = true)
public interface JwkSetUriJwtDecoderBuilderCustomizer extends
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.JwkSetUriJwtDecoderBuilderCustomizer {

}
