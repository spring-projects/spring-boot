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

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.HttpMapperProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Auto configuration for Jackson. The following auto-configuration will get applied:
 * <ul>
 * <li>an {@link ObjectMapper} in case none is already configured.</li>
 * <li>a {@link Jackson2ObjectMapperBuilder} in case none is already configured.</li>
 * <li>auto-registration for all {@link Module} beans with all {@link ObjectMapper} beans
 * (including the defaulted ones).</li>
 * </ul>
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @author Marcel Overdijk
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
	@ConditionalOnClass({ ObjectMapper.class, Jackson2ObjectMapperBuilder.class })
	static class JacksonObjectMapperAutoConfiguration {

		@Bean
		@Primary
		@ConditionalOnMissingBean(ObjectMapper.class)
		public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
			return builder.createXmlMapper(false).build();
		}

	}

	@Configuration
	@ConditionalOnClass({ ObjectMapper.class, Jackson2ObjectMapperBuilder.class })
	@EnableConfigurationProperties({ HttpMapperProperties.class, JacksonProperties.class })
	static class JacksonObjectMapperBuilderAutoConfiguration {

		@Autowired
		private HttpMapperProperties httpMapperProperties = new HttpMapperProperties();

		@Autowired
		private JacksonProperties jacksonProperties = new JacksonProperties();

		@Bean
		@ConditionalOnMissingBean(Jackson2ObjectMapperBuilder.class)
		public Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder() {
			Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

			if (this.httpMapperProperties.isJsonSortKeys()) {
				builder.featuresToEnable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
			}

			configureFeatures(builder, this.jacksonProperties.getDeserialization());
			configureFeatures(builder, this.jacksonProperties.getSerialization());
			configureFeatures(builder, this.jacksonProperties.getMapper());
			configureFeatures(builder, this.jacksonProperties.getParser());
			configureFeatures(builder, this.jacksonProperties.getGenerator());

			configureDateFormat(builder);
			configurePropertyNamingStrategy(builder);

			return builder;
		}

		private void configureFeatures(Jackson2ObjectMapperBuilder builder,
				Map<?, Boolean> features) {
			for (Entry<?, Boolean> entry : features.entrySet()) {
				if (entry.getValue() != null && entry.getValue()) {
					builder.featuresToEnable(entry.getKey());
				}
				else {
					builder.featuresToDisable(entry.getKey());
				}
			}
		}

		private void configurePropertyNamingStrategy(Jackson2ObjectMapperBuilder builder) {
			// We support a fully qualified class name extending Jackson's
			// PropertyNamingStrategy or a string value corresponding to the constant
			// names in PropertyNamingStrategy which hold default provided implementations
			String propertyNamingStrategy = this.jacksonProperties
					.getPropertyNamingStrategy();
			if (propertyNamingStrategy != null) {
				try {
					Class<?> clazz = ClassUtils.forName(propertyNamingStrategy, null);
					builder.propertyNamingStrategy((PropertyNamingStrategy) BeanUtils
							.instantiateClass(clazz));
				}
				catch (ClassNotFoundException e) {
					// Find the field (this way we automatically support new constants
					// that may be added by Jackson in the future)
					Field field = ReflectionUtils.findField(PropertyNamingStrategy.class,
							propertyNamingStrategy, PropertyNamingStrategy.class);
					if (field != null) {
						try {
							builder.propertyNamingStrategy((PropertyNamingStrategy) field
									.get(null));
						}
						catch (Exception ex) {
							throw new IllegalStateException(ex);
						}
					}
					else {
						throw new IllegalArgumentException("Constant named '"
								+ propertyNamingStrategy + "' not found on "
								+ PropertyNamingStrategy.class.getName());
					}
				}
			}
		}

		private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
			// We support a fully qualified class name extending DateFormat or a date
			// pattern string value
			String dateFormat = this.jacksonProperties.getDateFormat();
			if (dateFormat != null) {
				try {
					Class<?> clazz = ClassUtils.forName(dateFormat, null);
					builder.dateFormat((DateFormat) BeanUtils.instantiateClass(clazz));
				}
				catch (ClassNotFoundException e) {
					builder.dateFormat(new SimpleDateFormat(dateFormat));
				}
			}
		}

	}

}
