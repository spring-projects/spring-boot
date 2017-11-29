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

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
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

	private static final Log logger = LogFactory
			.getLog(ReactiveAuthenticationManagerConfiguration.class);

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService(
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		String password = UUID.randomUUID().toString();
		logger.info(String.format("%n%nUsing default security password: %s%n", password));
		UserDetails userDetails = getUserDetails(password, passwordEncoder);
		return new MapReactiveUserDetailsService(userDetails);
	}

	private UserDetails getUserDetails(String password,
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		String encodedPassword = passwordEncoder
				.getIfAvailable(PasswordEncoderFactories::createDelegatingPasswordEncoder)
				.encode(password);
		return User.withUsername("user").password(encodedPassword).roles().build();
	}

}
