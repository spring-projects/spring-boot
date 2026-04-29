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

package org.springframework.boot.servlet.autoconfigure;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * {@link AutoConfiguration} for adapting Forwarded-Headers in a servlet web application.
 *
 * @author Hans Hosea Schaefer
 * @since 4.0.7
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ForwardedHeaderFilter.class)
@Import(ForwardedHeaderAutoConfiguration.LegacyForwardedHeaderFilterCustomizerSupportConfiguration.class)
public final class ForwardedHeaderAutoConfiguration {

	@Bean
	@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "framework")
	@ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
	@ConditionalOnMissingClass("org.springframework.boot.web.server.autoconfigure.servlet.ForwardedHeaderFilterCustomizer")
	FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(
			final ObjectProvider<ForwardedHeaderFilterCustomizer> customizerProvider) {
		final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
		customizerProvider.ifAvailable((customizer) -> customizer.customize(filter));
		final FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	/**
	 * Support for ForwardedHeaderFilterCustomizer from optional spring-boot-web-server.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.springframework.boot.web.server.autoconfigure.servlet.ForwardedHeaderFilterCustomizer.class)
	static class LegacyForwardedHeaderFilterCustomizerSupportConfiguration {

		@Bean
		@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "framework")
		@ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
		FilterRegistrationBean<ForwardedHeaderFilter> legacyForwardedHeaderFilter(
				final ObjectProvider<ForwardedHeaderFilterCustomizer> customizerProvider,
				final ObjectProvider<org.springframework.boot.web.server.autoconfigure.servlet.ForwardedHeaderFilterCustomizer> legacyCustomizerProvider) {
			final ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
			customizerProvider.ifAvailable((customizer) -> customizer.customize(filter));
			legacyCustomizerProvider.ifAvailable((legacyCustomizer) -> legacyCustomizer.customize(filter));
			final FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return registration;
		}

	}

}
