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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.joda.time.DateTime;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpMessageConverter}s.
 * 
 * @author Dave Syer
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
	@ConditionalOnClass({ JodaModule.class, DateTime.class })
	protected static class JodaModuleConfiguration {

		@Bean
		public JodaModule jodaModule() {
			return new JodaModule();
		}

	}

	@Configuration
	@ConditionalOnClass(ObjectMapper.class)
	protected static class ObjectMappers {

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
			return converter;
		}

	}

}
