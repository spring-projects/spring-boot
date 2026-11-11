/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactive OAuth2 resource server
 * support.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(before = { ReactiveManagementWebSecurityAutoConfiguration.class,
		ReactiveWebSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class })
@ConditionalOnClass({ Mono.class, BearerTokenAuthenticationToken.class })
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@Import({ ReactiveJwtDecoderConfiguration.class, ReactiveJwtConverterConfiguration.class,
		ReactiveOpaqueTokenIntrospectionClientConfiguration.class })
public final class ReactiveOAuth2ResourceServerAutoConfiguration {

}
