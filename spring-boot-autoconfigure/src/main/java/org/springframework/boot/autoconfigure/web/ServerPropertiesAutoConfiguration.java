/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that configures the
 * {@link ConfigurableEmbeddedServletContainerFactory} from a {@link ServerProperties}
 * bean.
 * 
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties
public class ServerPropertiesAutoConfiguration implements ApplicationContextAware,
		EmbeddedServletContainerCustomizer {

	private ApplicationContext applicationContext;

	@Bean(name = "org.springframework.boot.autoconfigure.web.ServerProperties")
	@ConditionalOnMissingBean
	public ServerProperties serverProperties() {
		return new ServerProperties();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
		String[] serverPropertiesBeans = this.applicationContext
				.getBeanNamesForType(ServerProperties.class);
		Assert.state(
				serverPropertiesBeans.length == 1,
				"Multiple ServerProperties beans registered "
						+ StringUtils.arrayToCommaDelimitedString(serverPropertiesBeans));
	}

}
