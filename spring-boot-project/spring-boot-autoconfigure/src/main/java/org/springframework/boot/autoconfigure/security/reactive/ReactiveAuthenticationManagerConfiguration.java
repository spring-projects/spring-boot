/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Default user {@link Configuration} for a reactive web application. Configures a
 * {@link ReactiveUserDetailsService} with a default user and generated password. This
 * backs-off completely if there is a bean of type {@link ReactiveUserDetailsService} or
 * {@link ReactiveAuthenticationManager}.
 *
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnClass({ ReactiveAuthenticationManager.class })
@ConditionalOnMissingBean({ ReactiveAuthenticationManager.class,
		ReactiveUserDetailsService.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class ReactiveAuthenticationManagerConfiguration {

	private final Pattern pattern = Pattern.compile("^\\{.+}.*$");

	private static final String NOOP_PREFIX = "{noop}";

	private static final Log logger = LogFactory
			.getLog(ReactiveAuthenticationManagerConfiguration.class);

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService(
			SecurityProperties properties,
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		SecurityProperties.User user = properties.getUser();
		if (user.isPasswordGenerated()) {
			logger.info(String.format("%n%nUsing default security password: %s%n",
					user.getPassword()));
		}
		String password = deducePassword(passwordEncoder, user.getPassword());
		UserDetails userDetails = getUserDetails(user, password);
		return new MapReactiveUserDetailsService(userDetails);
	}

	private String deducePassword(ObjectProvider<PasswordEncoder> passwordEncoder, String password) {
		if (passwordEncoder.getIfAvailable() == null &&
				!this.pattern.matcher(password).matches()) {
			return NOOP_PREFIX + password;
		}
		return password;
	}

	private UserDetails getUserDetails(SecurityProperties.User user,
			String password) {
		List<String> roles = user.getRoles();
		return User.withUsername(user.getName()).password(password)
				.roles(roles.toArray(new String[roles.size()])).build();
	}

}
