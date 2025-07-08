/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.autoconfigure.actuate.servlet;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.security.autoconfigure.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ClassUtils;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security when actuator is
 * on the classpath. It allows unauthenticated access to the {@link HealthEndpoint}. If
 * the user specifies their own {@link SecurityFilterChain} bean, this will back-off
 * completely and the user should specify all the bits that they want to configure as part
 * of the custom security configuration.
 *
 * @author Madhura Bhave
 * @author Hatef Palizgar
 * @since 4.0.0
 */
@AutoConfiguration(before = SecurityAutoConfiguration.class,
		afterName = { "org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration",
				"org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration" })
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ RequestMatcher.class, WebEndpointAutoConfiguration.class })
@ConditionalOnDefaultWebSecurity
public class ManagementWebSecurityAutoConfiguration {

	@Bean
	@Order(SecurityProperties.BASIC_AUTH_ORDER)
	SecurityFilterChain managementSecurityFilterChain(Environment environment, HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((requests) -> {
			requests.requestMatchers(healthMatcher(), additionalHealthPathsMatcher()).permitAll();
			requests.anyRequest().authenticated();
		});
		if (ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet", null)) {
			http.cors(withDefaults());
		}
		http.formLogin(withDefaults());
		http.httpBasic(withDefaults());
		return http.build();
	}

	private RequestMatcher healthMatcher() {
		return EndpointRequest.to(HealthEndpoint.class);
	}

	private RequestMatcher additionalHealthPathsMatcher() {
		return EndpointRequest.toAdditionalPaths(WebServerNamespace.SERVER, HealthEndpoint.class);
	}

}
