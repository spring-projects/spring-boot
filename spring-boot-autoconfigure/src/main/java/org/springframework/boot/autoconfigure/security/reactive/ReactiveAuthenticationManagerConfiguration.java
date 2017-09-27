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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapUserDetailsRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsRepository;

/**
 * Default user {@link Configuration} for a reactive web application. Configures a
 * {@link UserDetailsRepository} with a default user and generated password. This
 * backs-off completely if there is a bean of type {@link UserDetailsRepository} or
 * {@link ReactiveAuthenticationManager}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ ReactiveAuthenticationManager.class })
@ConditionalOnMissingBean({ ReactiveAuthenticationManager.class,
		UserDetailsRepository.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveAuthenticationManagerConfiguration {

	private static final Log logger = LogFactory
			.getLog(ReactiveAuthenticationManagerConfiguration.class);

	@Bean
	public MapUserDetailsRepository userDetailsRepository() {
		String password = UUID.randomUUID().toString();
		logger.info(String.format("%n%nUsing default security password: %s%n", password));
		UserDetails user = User.withUsername("user").password(password).roles().build();
		return new MapUserDetailsRepository(user);
	}
}
