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
package org.springframework.zero.actuate.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.actuate.properties.ManagementServerProperties;
import org.springframework.zero.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.zero.context.annotation.AutoConfigureAfter;
import org.springframework.zero.context.annotation.ConditionalOnMissingBean;
import org.springframework.zero.context.annotation.EnableAutoConfiguration;
import org.springframework.zero.context.annotation.EnableConfigurationProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link ManagementServerPropertiesAutoConfiguration} bean.
 *
 * @author Dave Syer
 */
@Configuration
@AutoConfigureAfter(ServerPropertiesAutoConfiguration.class)
@EnableConfigurationProperties
public class ManagementServerPropertiesAutoConfiguration {

	@Bean(name = "org.springframework.zero.actuate.properties.ManagementServerProperties")
	@ConditionalOnMissingBean
	public ManagementServerProperties serverProperties() {
		return new ManagementServerProperties();
	}

}
