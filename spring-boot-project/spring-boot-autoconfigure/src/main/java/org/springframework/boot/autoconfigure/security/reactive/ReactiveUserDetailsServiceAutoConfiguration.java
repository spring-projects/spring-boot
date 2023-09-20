/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.reactive;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Default user {@link Configuration @Configuration} for a reactive web application.
 * Configures a {@link ReactiveUserDetailsService} with a default user and generated
 * password. This backs-off completely if there is a bean of type
 * {@link ReactiveUserDetailsService}, {@link ReactiveAuthenticationManager}, or
 * {@link ReactiveAuthenticationManagerResolver}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@AutoConfiguration(before = ReactiveSecurityAutoConfiguration.class, after = RSocketMessagingAutoConfiguration.class)
@ConditionalOnClass({ ReactiveAuthenticationManager.class })
@ConditionalOnMissingClass({ "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository",
		"org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector" })
@ConditionalOnMissingBean(
		value = { ReactiveAuthenticationManager.class, ReactiveUserDetailsService.class,
				ReactiveAuthenticationManagerResolver.class },
		type = { "org.springframework.security.oauth2.jwt.ReactiveJwtDecoder" })
@Conditional(ReactiveUserDetailsServiceAutoConfiguration.ReactiveUserDetailsServiceCondition.class)
@EnableConfigurationProperties(SecurityProperties.class)
public class ReactiveUserDetailsServiceAutoConfiguration {

	private static final String NOOP_PASSWORD_PREFIX = "{noop}";

	private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

	private static final Log logger = LogFactory.getLog(ReactiveUserDetailsServiceAutoConfiguration.class);

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService(SecurityProperties properties,
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		SecurityProperties.User user = properties.getUser();
		UserDetails userDetails = getUserDetails(user, getOrDeducePassword(user, passwordEncoder.getIfAvailable()));
		return new MapReactiveUserDetailsService(userDetails);
	}

	private UserDetails getUserDetails(SecurityProperties.User user, String password) {
		List<String> roles = user.getRoles();
		return User.withUsername(user.getName()).password(password).roles(StringUtils.toStringArray(roles)).build();
	}

	private String getOrDeducePassword(SecurityProperties.User user, PasswordEncoder encoder) {
		String password = user.getPassword();
		if (user.isPasswordGenerated()) {
			logger.info(String.format("%n%nUsing generated security password: %s%n", user.getPassword()));
		}
		if (encoder != null || PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
			return password;
		}
		return NOOP_PASSWORD_PREFIX + password;
	}

	static class ReactiveUserDetailsServiceCondition extends AnyNestedCondition {

		ReactiveUserDetailsServiceCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(RSocketMessageHandler.class)
		static class RSocketSecurityEnabledCondition {

		}

		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
		static class ReactiveWebApplicationCondition {

		}

	}

}
