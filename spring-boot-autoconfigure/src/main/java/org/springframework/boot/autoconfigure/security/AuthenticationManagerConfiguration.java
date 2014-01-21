/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;

@Configuration
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean(AuthenticationManager.class)
public class AuthenticationManagerConfiguration {

	private static Log logger = LogFactory
			.getLog(AuthenticationManagerConfiguration.class);

	@Autowired
	private SecurityProperties security;

	@Autowired
	private List<SecurityPrequisite> dependencies;

	@Bean
	public AuthenticationManager authenticationManager(
			ObjectPostProcessor<Object> objectPostProcessor) throws Exception {

		InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> builder = new AuthenticationManagerBuilder(
				objectPostProcessor).inMemoryAuthentication();
		User user = this.security.getUser();

		if (user.isDefaultPassword()) {
			logger.info("\n\nUsing default password for application endpoints: "
					+ user.getPassword() + "\n\n");
		}

		Set<String> roles = new LinkedHashSet<String>(user.getRole());

		builder.withUser(user.getName()).password(user.getPassword())
				.roles(roles.toArray(new String[roles.size()]));

		return builder.and().build();

	}

}