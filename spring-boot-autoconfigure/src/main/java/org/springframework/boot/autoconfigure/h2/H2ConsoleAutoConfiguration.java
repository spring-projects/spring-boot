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

package org.springframework.boot.autoconfigure.h2;

import org.h2.server.web.WebServlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for H2's web console.
 *
 * @author Andy Wilkinson
 * @author Marten Deinum
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(WebServlet.class)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(H2ConsoleProperties.class)
@AutoConfigureAfter(SecurityAutoConfiguration.class)
public class H2ConsoleAutoConfiguration {

	private final H2ConsoleProperties properties;

	public H2ConsoleAutoConfiguration(H2ConsoleProperties properties) {
		this.properties = properties;
	}

	@Bean
	public ServletRegistrationBean<WebServlet> h2Console() {
		String path = this.properties.getPath();
		String urlMapping = (path.endsWith("/") ? path + "*" : path + "/*");
		ServletRegistrationBean<WebServlet> registration = new ServletRegistrationBean<>(
				new WebServlet(), urlMapping);
		H2ConsoleProperties.Settings settings = this.properties.getSettings();
		if (settings.isTrace()) {
			registration.addInitParameter("trace", "");
		}
		if (settings.isWebAllowOthers()) {
			registration.addInitParameter("webAllowOthers", "");
		}
		return registration;
	}

	@Configuration
	@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
	@ConditionalOnBean(ObjectPostProcessor.class)
	@ConditionalOnProperty(prefix = "security.basic", name = "enabled", matchIfMissing = true)
	static class H2ConsoleSecurityConfiguration {

		@Bean
		public WebSecurityConfigurerAdapter h2ConsoleSecurityConfigurer() {
			return new H2ConsoleSecurityConfigurer();
		}

		@Order(SecurityProperties.BASIC_AUTH_ORDER - 10)
		private static class H2ConsoleSecurityConfigurer
				extends WebSecurityConfigurerAdapter {

			@Autowired
			private H2ConsoleProperties console;

			@Override
			public void configure(HttpSecurity http) throws Exception {
				String path = this.console.getPath();
				String antPattern = (path.endsWith("/") ? path + "**" : path + "/**");
				HttpSecurity h2Console = http.antMatcher(antPattern);
				h2Console.csrf().disable();
				h2Console.httpBasic();
				h2Console.headers().frameOptions().sameOrigin();
				http.authorizeRequests().anyRequest().authenticated();
			}

		}

	}

}
