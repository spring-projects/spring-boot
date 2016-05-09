/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security's Filter.
 * Configured separately from {@link SpringBootWebSecurityConfiguration} to ensure that
 * the filter's order is still configured when a user-provided
 * {@link WebSecurityConfiguration} exists.
 *
 * @author Rob Winch
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties
@ConditionalOnClass({ AbstractSecurityWebApplicationInitializer.class,
		SessionCreationPolicy.class })
@AutoConfigureAfter(SpringBootWebSecurityConfiguration.class)
public class SecurityFilterAutoConfiguration {

	private static final String DEFAULT_FILTER_NAME = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME;

	@Bean
	@ConditionalOnBean(name = DEFAULT_FILTER_NAME)
	public DelegatingFilterProxyRegistrationBean securityFilterChainRegistration(
			SecurityProperties securityProperties) {
		DelegatingFilterProxyRegistrationBean registration = new DelegatingFilterProxyRegistrationBean(
				DEFAULT_FILTER_NAME);
		registration.setOrder(securityProperties.getFilterOrder());
		registration.setDispatcherTypes(getDispatcherTypes(securityProperties));
		return registration;
	}

	@Bean
	@ConditionalOnMissingBean
	public SecurityProperties securityProperties() {
		return new SecurityProperties();
	}

	private EnumSet<DispatcherType> getDispatcherTypes(
			SecurityProperties securityProperties) {
		if (securityProperties.getFilterDispatcherTypes() == null) {
			return null;
		}
		Set<DispatcherType> dispatcherTypes = new HashSet<DispatcherType>();
		for (String dispatcherType : securityProperties.getFilterDispatcherTypes()) {
			dispatcherTypes.add(DispatcherType.valueOf(dispatcherType));
		}
		return EnumSet.copyOf(dispatcherTypes);
	}

}
