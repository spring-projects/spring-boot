/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's web support.
 * <p>
 * When in effect, the auto-configuration is the equivalent of enabling Spring Data's web
 * support through the {@link EnableSpringDataWebSupport @EnableSpringDataWebSupport}
 * annotation.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @since 1.2.0
 */
@AutoConfiguration(after = RepositoryRestMvcAutoConfiguration.class)
@EnableSpringDataWebSupport
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ PageableHandlerMethodArgumentResolver.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(PageableHandlerMethodArgumentResolver.class)
@EnableConfigurationProperties(SpringDataWebProperties.class)
public class SpringDataWebAutoConfiguration {

	private final SpringDataWebProperties properties;

	public SpringDataWebAutoConfiguration(SpringDataWebProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
		return (resolver) -> {
			Pageable pageable = this.properties.getPageable();
			resolver.setPageParameterName(pageable.getPageParameter());
			resolver.setSizeParameterName(pageable.getSizeParameter());
			resolver.setOneIndexedParameters(pageable.isOneIndexedParameters());
			resolver.setPrefix(pageable.getPrefix());
			resolver.setQualifierDelimiter(pageable.getQualifierDelimiter());
			resolver.setFallbackPageable(PageRequest.of(0, pageable.getDefaultPageSize()));
			resolver.setMaxPageSize(pageable.getMaxPageSize());
		};
	}

	@Bean
	@ConditionalOnMissingBean
	public SortHandlerMethodArgumentResolverCustomizer sortCustomizer() {
		return (resolver) -> resolver.setSortParameter(this.properties.getSort().getSortParameter());
	}

}
