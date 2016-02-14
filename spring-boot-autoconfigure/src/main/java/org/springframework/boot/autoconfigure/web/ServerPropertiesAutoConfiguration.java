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

package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that configures the
 * {@link ConfigurableEmbeddedServletContainer} from a {@link ServerProperties} bean.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnWebApplication
public class ServerPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public ServerProperties serverProperties() {
		return new ServerProperties();
	}

	@Bean
	public DuplicateServerPropertiesDetector duplicateServerPropertiesDetector() {
		return new DuplicateServerPropertiesDetector();
	}

	/**
	 * {@link EmbeddedServletContainerCustomizer} that ensures there is exactly one
	 * {@link ServerProperties} bean in the application context.
	 */
	private static class DuplicateServerPropertiesDetector implements
			EmbeddedServletContainerCustomizer, Ordered, ApplicationContextAware {

		private ApplicationContext applicationContext;

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public void customize(ConfigurableEmbeddedServletContainer container) {
			// ServerProperties handles customization, this just checks we only have
			// a single bean
			String[] serverPropertiesBeans = this.applicationContext
					.getBeanNamesForType(ServerProperties.class);
			Assert.state(serverPropertiesBeans.length == 1,
					"Multiple ServerProperties beans registered " + StringUtils
							.arrayToCommaDelimitedString(serverPropertiesBeans));
		}

	}

}
