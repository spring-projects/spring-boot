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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
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
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class DevceDelegatingViewResolverAutoConfiguration {

	private static Log logger = LogFactory.getLog(WebMvcConfigurerAdapter.class);

	public static final String DEFAULT_NORMAL_PREFIX = "";

	public static final String DEFAULT_MOBILE_PREFIX = "mobile/";

	public static final String DEFAULT_TABLET_PREFIX = "tablet/";

	public static final String DEFAULT_NORMAL_SUFFIX = "";

	public static final String DEFAULT_MOBILE_SUFFIX = "";

	public static final String DEFAULT_TABLET_SUFFIX = "";

	@Configuration
	@ConditionalOnMissingBean(name = "deviceDelegatingViewResolver")
	@ConditionalOnExpression("${spring.mobile.deviceDelegatingViewResolver.enabled:false}")
	protected static class DevceDelegatingViewResolverConfiguration {

		@Configuration
		@ConditionalOnBean(ThymeleafViewResolver.class)
		@AutoConfigureAfter(ThymeleafAutoConfiguration.class)
		protected static class ThymeleafViewResolverViewResolverDelegateConfiguration
				extends AbstractDelegateConfiguration {

			@Autowired
			private ThymeleafViewResolver thymeleafViewResolver;

			@Bean
			public LiteDeviceDelegatingViewResolver deviceDelegatingViewResolver() {
				if (logger.isDebugEnabled()) {
					logger.debug("LiteDeviceDelegatingViewResolver delegates to ThymeleafViewResolver");
				}
				return getConfiguredViewResolver(thymeleafViewResolver,
						thymeleafViewResolver.getOrder());
			}

		}

		@Configuration
		@ConditionalOnMissingBean(ThymeleafViewResolver.class)
		@ConditionalOnBean(InternalResourceViewResolver.class)
		protected static class InternalResourceViewResolverDelegateConfiguration extends
				AbstractDelegateConfiguration {

			@Autowired
			private InternalResourceViewResolver internalResourceViewResolver;

			@Bean
			public LiteDeviceDelegatingViewResolver deviceDelegatingViewResolver() {
				if (logger.isDebugEnabled()) {
					logger.debug("LiteDeviceDelegatingViewResolver delegates to InternalResourceViewResolver");
				}
				return getConfiguredViewResolver(internalResourceViewResolver,
						internalResourceViewResolver.getOrder());
			}

		}

		private static abstract class AbstractDelegateConfiguration implements
				EnvironmentAware {

			private RelaxedPropertyResolver environment;

			@Override
			public void setEnvironment(Environment environment) {
				this.environment = new RelaxedPropertyResolver(environment,
						"spring.mobile.deviceDelegatingViewResolver.");
			}

			protected LiteDeviceDelegatingViewResolver getConfiguredViewResolver(
					ViewResolver delegate, int delegateOrder) {
				LiteDeviceDelegatingViewResolver resolver = new LiteDeviceDelegatingViewResolver(
						delegate);
				resolver.setNormalPrefix(this.environment.getProperty("normalPrefix",
						DEFAULT_NORMAL_PREFIX));
				resolver.setMobilePrefix(this.environment.getProperty("mobilePrefix",
						DEFAULT_MOBILE_PREFIX));
				resolver.setTabletPrefix(this.environment.getProperty("tabletPrefix",
						DEFAULT_TABLET_PREFIX));
				resolver.setNormalSuffix(this.environment.getProperty("normalSuffix",
						DEFAULT_NORMAL_SUFFIX));
				resolver.setMobileSuffix(this.environment.getProperty("mobileSuffix",
						DEFAULT_MOBILE_SUFFIX));
				resolver.setTabletSuffix(this.environment.getProperty("tabletSuffix",
						DEFAULT_TABLET_SUFFIX));
				resolver.setOrder(getAdjustedOrder(delegateOrder));
				return resolver;
			}

			private int getAdjustedOrder(int delegateViewResolverOrder) {
				if (delegateViewResolverOrder == Ordered.HIGHEST_PRECEDENCE) {
					return Ordered.HIGHEST_PRECEDENCE;
				} else {
					// The view resolver must be ordered higher than the delegate view
					// resolver, otherwise the view names will not be adjusted
					return delegateViewResolverOrder - 1;
				}
			}

		}

	}

}
