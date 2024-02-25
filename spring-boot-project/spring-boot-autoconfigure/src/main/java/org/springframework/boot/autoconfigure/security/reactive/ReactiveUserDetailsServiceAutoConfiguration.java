/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnMissingBean(
		value = { ReactiveAuthenticationManager.class, ReactiveUserDetailsService.class,
				ReactiveAuthenticationManagerResolver.class },
		type = { "org.springframework.security.oauth2.jwt.ReactiveJwtDecoder" })
@Conditional({ ReactiveUserDetailsServiceAutoConfiguration.RSocketEnabledOrReactiveWebApplication.class,
		ReactiveUserDetailsServiceAutoConfiguration.MissingAlternativeOrUserPropertiesConfigured.class })
@EnableConfigurationProperties(SecurityProperties.class)
public class ReactiveUserDetailsServiceAutoConfiguration {

	private static final String NOOP_PASSWORD_PREFIX = "{noop}";

	private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

	private static final Log logger = LogFactory.getLog(ReactiveUserDetailsServiceAutoConfiguration.class);

	/**
	 * Creates a reactive user details service using the provided security properties and
	 * password encoder.
	 * @param properties the security properties containing user details
	 * @param passwordEncoder the password encoder used to encode the user's password
	 * @return a reactive user details service with the specified user details
	 */
	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService(SecurityProperties properties,
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		SecurityProperties.User user = properties.getUser();
		UserDetails userDetails = getUserDetails(user, getOrDeducePassword(user, passwordEncoder.getIfAvailable()));
		return new MapReactiveUserDetailsService(userDetails);
	}

	/**
	 * Retrieves the user details for the given user and password.
	 * @param user the security user object containing user details
	 * @param password the password for the user
	 * @return the user details object
	 */
	private UserDetails getUserDetails(SecurityProperties.User user, String password) {
		List<String> roles = user.getRoles();
		return User.withUsername(user.getName()).password(password).roles(StringUtils.toStringArray(roles)).build();
	}

	/**
	 * Retrieves the password for the given user or deduces it based on the provided
	 * encoder. If the user's password is generated, it logs the generated password. If an
	 * encoder is provided or the password matches the password algorithm pattern, it
	 * returns the password as is. Otherwise, it returns the password prefixed with
	 * NOOP_PASSWORD_PREFIX.
	 * @param user the user for which to retrieve or deduce the password
	 * @param encoder the password encoder to use for deducing the password
	 * @return the retrieved or deduced password
	 */
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

	/**
	 * RSocketEnabledOrReactiveWebApplication class.
	 */
	static class RSocketEnabledOrReactiveWebApplication extends AnyNestedCondition {

		/**
		 * Constructor for the RSocketEnabledOrReactiveWebApplication class.
		 * @param configurationPhase The configuration phase for registering the bean.
		 */
		RSocketEnabledOrReactiveWebApplication() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
		 * RSocketSecurityEnabledCondition class.
		 */
		@ConditionalOnBean(RSocketMessageHandler.class)
		static class RSocketSecurityEnabledCondition {

		}

		/**
		 * ReactiveWebApplicationCondition class.
		 */
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
		static class ReactiveWebApplicationCondition {

		}

	}

	/**
	 * MissingAlternativeOrUserPropertiesConfigured class.
	 */
	static final class MissingAlternativeOrUserPropertiesConfigured extends AnyNestedCondition {

		/**
		 * Constructs a new instance of the MissingAlternativeOrUserPropertiesConfigured
		 * class.
		 *
		 * This constructor calls the super constructor with the
		 * ConfigurationPhase.PARSE_CONFIGURATION parameter.
		 */
		MissingAlternativeOrUserPropertiesConfigured() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		/**
		 * MissingAlternative class.
		 */
		@ConditionalOnMissingClass({
				"org.springframework.security.oauth2.client.registration.ClientRegistrationRepository",
				"org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector" })
		static final class MissingAlternative {

		}

		/**
		 * NameConfigured class.
		 */
		@ConditionalOnProperty(prefix = "spring.security.user", name = "name")
		static final class NameConfigured {

		}

		/**
		 * PasswordConfigured class.
		 */
		@ConditionalOnProperty(prefix = "spring.security.user", name = "password")
		static final class PasswordConfigured {

		}

	}

}
