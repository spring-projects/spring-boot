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

package org.springframework.boot.data.rest.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data REST.
 * <p>
 * Activates when the application is a web application and no
 * {@link RepositoryRestMvcConfiguration} is found.
 * <p>
 * Once in effect, the auto-configuration allows to configure any property of
 * {@link RepositoryRestConfiguration} using the {@code spring.data.rest} prefix.
 *
 * @author Rob Winch
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(before = DataWebAutoConfiguration.class, after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnMissingBean(RepositoryRestMvcConfiguration.class)
@ConditionalOnClass(RepositoryRestMvcConfiguration.class)
@EnableConfigurationProperties(DataRestProperties.class)
@Import(RepositoryRestMvcConfiguration.class)
public final class DataRestAutoConfiguration {

	@Bean
	SpringBootRepositoryRestConfigurer springBootRepositoryRestConfigurer(
			ObjectProvider<JsonMapperBuilderCustomizer> jsonMapperBuilderCustomizers, DataRestProperties properties) {
		return new SpringBootRepositoryRestConfigurer(jsonMapperBuilderCustomizers.orderedStream().toList(),
				properties);
	}

}
