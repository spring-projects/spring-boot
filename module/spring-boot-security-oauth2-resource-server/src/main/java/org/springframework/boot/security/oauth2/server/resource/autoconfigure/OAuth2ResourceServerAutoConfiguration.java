/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth2 resource server support.
 *
 * @author Madhura Bhave
 * @since 4.1.0
 */
@AutoConfiguration(before = { SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class })
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@Import({ JwtConverterConfiguration.class, JwtDecoderConfiguration.class, OpaqueTokenIntrospectionConfiguration.class })
public final class OAuth2ResourceServerAutoConfiguration {

}
