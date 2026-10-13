/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactive OAuth2 resource server
 * support.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(before = { ReactiveManagementWebSecurityAutoConfiguration.class,
		ReactiveWebSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class })
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@ConditionalOnClass({ EnableWebFluxSecurity.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@Import({ ReactiveOAuth2ResourceServerConfiguration.JwtConfiguration.class,
		ReactiveOAuth2ResourceServerConfiguration.OpaqueTokenConfiguration.class })
public final class ReactiveOAuth2ResourceServerAutoConfiguration {

}
