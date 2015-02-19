/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;

/**
 * This works with the {@link AuthenticationConfiguration} to ensure that users are able
 * to use:
 *
 * <pre>
 * public void configureGlobal(AuthenticationManagerBuilder auth) {
 *     ...
 * }
 * </pre>
 *
 * within their classes annotated with {@link EnableAutoConfiguration} or
 * {@link SpringBootApplication}.
 *
 * @author Rob Winch
 * @since 1.1.11
 */
@Configuration
@ConditionalOnClass(GlobalAuthenticationConfigurerAdapter.class)
public class BootGlobalAuthenticationConfiguration {

	@Bean
	public static BootGlobalAuthenticationConfigurationAdapter bootGlobalAuthenticationConfigurationAdapter(
			ApplicationContext context) {
		return new BootGlobalAuthenticationConfigurationAdapter(context);
	}

	private static class BootGlobalAuthenticationConfigurationAdapter extends
			GlobalAuthenticationConfigurerAdapter {

		private static Log logger = LogFactory
				.getLog(BootGlobalAuthenticationConfiguration.class);

		private final ApplicationContext context;

		public BootGlobalAuthenticationConfigurationAdapter(ApplicationContext context) {
			this.context = context;
		}

		@Override
		public void init(AuthenticationManagerBuilder auth) {
			Map<String, Object> beansWithAnnotation = this.context
					.getBeansWithAnnotation(EnableAutoConfiguration.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly initializing " + beansWithAnnotation);
			}
		}
	}
}
