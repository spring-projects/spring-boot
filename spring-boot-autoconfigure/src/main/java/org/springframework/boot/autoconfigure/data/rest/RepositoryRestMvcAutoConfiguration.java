/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.rest;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data Rest's MVC
 * integration.
 * <p>
 * Activates when the application is a web application and no
 * {@link RepositoryRestMvcConfiguration} is found.
 * </p>
 * <p>
 * Once in effect, the auto-configuration allows to configure any property
 * of {@link RepositoryRestConfiguration} using the {@code spring.data.rest}
 * prefix.
 * </p>
 *
 * @author Rob Winch
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnMissingBean(RepositoryRestMvcConfiguration.class)
@ConditionalOnClass(RepositoryRestMvcConfiguration.class)
@Import(RepositoryRestMvcAutoConfiguration.RepositoryRestMvcBootConfiguration.class)
public class RepositoryRestMvcAutoConfiguration {


	@Configuration
	static class RepositoryRestMvcBootConfiguration extends RepositoryRestMvcConfiguration {

		@Override
		@Bean
		@ConfigurationProperties(prefix = "spring.data.rest")
		public RepositoryRestConfiguration config() {
			return super.config();
		}
	}
}
