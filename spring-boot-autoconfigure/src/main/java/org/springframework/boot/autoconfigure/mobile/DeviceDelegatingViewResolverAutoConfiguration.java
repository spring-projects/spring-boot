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

package org.springframework.boot.autoconfigure.mobile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Mobile's
 * {@link LiteDeviceDelegatingViewResolver}. If {@link ThymeleafViewResolver} is available
 * it is configured as the delegate view resolver. Otherwise,
 * {@link InternalResourceViewResolver} is used as a fallback.
 * 
 * @author Roy Clarkson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(LiteDeviceDelegatingViewResolver.class)
@AutoConfigureAfter({ WebMvcAutoConfiguration.class, ThymeleafAutoConfiguration.class })
public class DeviceDelegatingViewResolverAutoConfiguration {

	private static Log logger = LogFactory.getLog(DeviceDelegatingViewResolverAutoConfiguration.class);

	private static abstract class AbstractDelegateConfiguration implements
			EnvironmentAware {

		private RelaxedPropertyResolver environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = new RelaxedPropertyResolver(environment,
					"spring.mobile.devicedelegatingviewresolver.");
		}

		protected LiteDeviceDelegatingViewResolver getConfiguredViewResolver(
				ViewResolver delegate, int delegateOrder) {
			LiteDeviceDelegatingViewResolver resolver = new LiteDeviceDelegatingViewResolver(
					delegate);
			resolver.setNormalPrefix(getProperty("normal-prefix", ""));
			resolver.setNormalSuffix(getProperty("normal-suffix", ""));
			resolver.setMobilePrefix(getProperty("mobile-prefix", "mobile/"));
			resolver.setMobileSuffix(getProperty("mobile-suffix", ""));
			resolver.setTabletPrefix(getProperty("tablet-prefix", "tablet/"));
			resolver.setTabletSuffix(getProperty("tablet-suffix", ""));
			resolver.setOrder(getAdjustedOrder(delegateOrder));
			return resolver;
		}

		private String getProperty(String key, String defaultValue) {
			return this.environment.getProperty(key, defaultValue);
		}

		private int getAdjustedOrder(int order) {
			if (order == Ordered.HIGHEST_PRECEDENCE) {
				return Ordered.HIGHEST_PRECEDENCE;
			}
			// The view resolver must be ordered higher than the delegate view
			// resolver, otherwise the view names will not be adjusted
			return order - 1;
		}

	}

	@Configuration
	@ConditionalOnMissingBean(name = "deviceDelegatingViewResolver")
	@ConditionalOnExpression("${spring.mobile.devicedelegatingviewresolver.enabled:false}")
	protected static class DeviceDelegatingViewResolverConfiguration {

		@Configuration
		@ConditionalOnBean(name = "thymeleafViewResolver")
		protected static class ThymeleafViewResolverViewResolverDelegateConfiguration
				extends AbstractDelegateConfiguration {

			@Autowired
			private ThymeleafViewResolver viewResolver;

			@Bean
			public LiteDeviceDelegatingViewResolver deviceDelegatingViewResolver() {
				if (logger.isDebugEnabled()) {
					logger.debug("LiteDeviceDelegatingViewResolver delegates to "
							+ "ThymeleafViewResolver");
				}
				return getConfiguredViewResolver(this.viewResolver,
						this.viewResolver.getOrder());
			}

		}

		@Configuration
		@ConditionalOnMissingBean(name = "thymeleafViewResolver")
		@ConditionalOnBean(InternalResourceViewResolver.class)
		protected static class InternalResourceViewResolverDelegateConfiguration extends
				AbstractDelegateConfiguration {

			@Autowired
			private InternalResourceViewResolver viewResolver;

			@Bean
			public LiteDeviceDelegatingViewResolver deviceDelegatingViewResolver() {
				if (logger.isDebugEnabled()) {
					logger.debug("LiteDeviceDelegatingViewResolver delegates to "
							+ "InternalResourceViewResolver");
				}
				return getConfiguredViewResolver(this.viewResolver,
						this.viewResolver.getOrder());
			}

		}

	}

}
