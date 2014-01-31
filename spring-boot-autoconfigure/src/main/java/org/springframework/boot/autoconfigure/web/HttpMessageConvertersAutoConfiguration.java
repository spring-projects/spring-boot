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

package org.springframework.boot.autoconfigure.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpMessageConverter}s.
 * 
 * @author Dave Syer
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnClass(HttpMessageConverter.class)
public class HttpMessageConvertersAutoConfiguration {

	@Autowired(required = false)
	private final List<HttpMessageConverter<?>> converters = Collections.emptyList();

	@Bean
	@ConditionalOnMissingBean
	public HttpMessageConverters messageConverters() {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>(
				this.converters);
		return new HttpMessageConverters(converters);
	}

	@Configuration
	@ConditionalOnClass(ObjectMapper.class)
	@EnableConfigurationProperties(ObjectMappersProperties.class)
	protected static class ObjectMappers {

		@Autowired
		private ObjectMappersProperties properties = new ObjectMappersProperties();

		@Autowired
		private ListableBeanFactory beanFactory;

		@PostConstruct
		public void init() {
			Collection<ObjectMapper> mappers = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(this.beanFactory, ObjectMapper.class)
					.values();
			Collection<Module> modules = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					this.beanFactory, Module.class).values();
			for (ObjectMapper mapper : mappers) {
				mapper.registerModules(modules);
			}
		}

		@Bean
		@ConditionalOnMissingBean
		@Primary
		public ObjectMapper jacksonObjectMapper() {
			return new ObjectMapper();
		}

		@Bean
		@ConditionalOnMissingBean
		public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(
				ObjectMapper objectMapper) {
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			converter.setObjectMapper(objectMapper);
			converter.setPrettyPrint(this.properties.isJsonPrettyPrint());
			return converter;
		}

	}

	/**
	 * {@link ConfigurationProperties} to configure {@link HttpMessageConverter}s.
	 */
	@ConfigurationProperties(name = "http.mappers", ignoreUnknownFields = false)
	public static class ObjectMappersProperties {

		private boolean jsonPrettyPrint = false;

		public void setJsonPrettyPrint(boolean jsonPrettyPrint) {
			this.jsonPrettyPrint = jsonPrettyPrint;
		}

		public boolean isJsonPrettyPrint() {
			return this.jsonPrettyPrint;
		}
	}

}
