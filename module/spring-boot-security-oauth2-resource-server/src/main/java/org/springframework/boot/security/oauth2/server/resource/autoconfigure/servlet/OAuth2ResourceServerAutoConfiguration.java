/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth2 resource server support.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(before = { ManagementWebSecurityAutoConfiguration.class, SecurityAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class })
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({ Oauth2ResourceServerConfiguration.JwtConfiguration.class,
		Oauth2ResourceServerConfiguration.OpaqueTokenConfiguration.class })
public final class OAuth2ResourceServerAutoConfiguration {

}
