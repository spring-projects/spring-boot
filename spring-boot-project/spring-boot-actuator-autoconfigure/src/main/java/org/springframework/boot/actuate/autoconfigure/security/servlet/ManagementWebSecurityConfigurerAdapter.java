/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * The default configuration for web security when the actuator dependency is on the
 * classpath. It is different from
 * {@link org.springframework.boot.autoconfigure.security.servlet.SpringBootWebSecurityConfiguration}
 * in that it allows unauthenticated access to the {@link HealthEndpoint} and
 * {@link InfoEndpoint}. If the user specifies their own
 * {@link WebSecurityConfigurerAdapter}, this will back-off completely and the user should
 * specify all the bits that they want to configure as part of the custom security
 * configuration.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
class ManagementWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests((requests) -> {
			requests.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll();
			requests.anyRequest().authenticated();
		});
		http.formLogin(Customizer.withDefaults());
		http.httpBasic(Customizer.withDefaults());
	}

}
