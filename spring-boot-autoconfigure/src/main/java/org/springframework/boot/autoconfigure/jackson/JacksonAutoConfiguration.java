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

package org.springframework.boot.autoconfigure.jackson;

import java.util.Collection;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.JavaVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.HttpMapperProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link ObjectMapper} in case none is already configured.</li>
 * <li>the {@link JodaModule} registered if it's on the classpath.</li>
 * <li>the {@link JSR310Module} registered if it's on the classpath and the application is
 * running on Java 8 or better.</li>
 * <li>auto-registration for all {@link Module} beans with all {@link ObjectMapper} beans
 * (including the defaulted ones).</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonAutoConfiguration {

	@Autowired
	private ListableBeanFactory beanFactory;

	@PostConstruct
	private void registerModulesWithObjectMappers() {
		Collection<Module> modules = getBeans(Module.class);
		for (ObjectMapper objectMapper : getBeans(ObjectMapper.class)) {
			objectMapper.registerModules(modules);
		}
	}

	private <T> Collection<T> getBeans(Class<T> type) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type)
				.values();
	}

	@Configuration
	@ConditionalOnClass(ObjectMapper.class)
	@EnableConfigurationProperties({ HttpMapperProperties.class, JacksonProperties.class })
	static class JacksonObjectMapperAutoConfiguration {

		@Autowired
		private HttpMapperProperties httpMapperProperties = new HttpMapperProperties();

		@Autowired
		private JacksonProperties jacksonProperties = new JacksonProperties();

		@Bean
		@Primary
		@ConditionalOnMissingBean
		public ObjectMapper jacksonObjectMapper() {
			ObjectMapper objectMapper = new ObjectMapper();

			if (this.httpMapperProperties.isJsonSortKeys()) {
				objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
						true);
			}

			configureDeserializationFeatures(objectMapper);
			configureSerializationFeatures(objectMapper);
			configureMapperFeatures(objectMapper);
			configureParserFeatures(objectMapper);
			configureGeneratorFeatures(objectMapper);

			return objectMapper;
		}

		private void configureDeserializationFeatures(ObjectMapper objectMapper) {
			for (Entry<DeserializationFeature, Boolean> entry : this.jacksonProperties
					.getDeserialization().entrySet()) {
				objectMapper.configure(entry.getKey(), isFeatureEnabled(entry));
			}
		}

		private void configureSerializationFeatures(ObjectMapper objectMapper) {
			for (Entry<SerializationFeature, Boolean> entry : this.jacksonProperties
					.getSerialization().entrySet()) {
				objectMapper.configure(entry.getKey(), isFeatureEnabled(entry));
			}
		}

		private void configureMapperFeatures(ObjectMapper objectMapper) {
			for (Entry<MapperFeature, Boolean> entry : this.jacksonProperties.getMapper()
					.entrySet()) {
				objectMapper.configure(entry.getKey(), isFeatureEnabled(entry));
			}
		}

		private void configureParserFeatures(ObjectMapper objectMapper) {
			for (Entry<JsonParser.Feature, Boolean> entry : this.jacksonProperties
					.getParser().entrySet()) {
				objectMapper.configure(entry.getKey(), isFeatureEnabled(entry));
			}
		}

		private void configureGeneratorFeatures(ObjectMapper objectMapper) {
			for (Entry<JsonGenerator.Feature, Boolean> entry : this.jacksonProperties
					.getGenerator().entrySet()) {
				objectMapper.configure(entry.getKey(), isFeatureEnabled(entry));
			}
		}

		private boolean isFeatureEnabled(Entry<?, Boolean> entry) {
			return entry.getValue() != null && entry.getValue();
		}
	}

	@Configuration
	@ConditionalOnClass(JodaModule.class)
	static class JodaModuleAutoConfiguration {

		@Bean
		@ConditionalOnMissingBean
		JodaModule jacksonJodaModule() {
			return new JodaModule();
		}

	}

	@Configuration
	@ConditionalOnJava(JavaVersion.EIGHT)
	@ConditionalOnClass(JSR310Module.class)
	static class Jsr310ModuleAutoConfiguration {

		@Bean
		@ConditionalOnMissingBean
		JSR310Module jacksonJsr310Module() {
			return new JSR310Module();
		}

	}

}
