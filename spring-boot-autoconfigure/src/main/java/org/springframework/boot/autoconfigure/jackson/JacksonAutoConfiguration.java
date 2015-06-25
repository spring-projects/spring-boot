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

package org.springframework.boot.autoconfigure.jackson;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;

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
 * @author Marcel Overdijk
 * @author Sebastien Deleuze
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonAutoConfiguration {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Configuration
	@ConditionalOnClass({ ObjectMapper.class, Jackson2ObjectMapperBuilder.class })
	static class JacksonObjectMapperConfiguration {

		@Bean
		@Primary
		@ConditionalOnMissingBean(ObjectMapper.class)
		public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
			return builder.createXmlMapper(false).build();
		}

	}

	@Configuration
	@ConditionalOnClass({ Jackson2ObjectMapperBuilder.class, DateTime.class,
			DateTimeSerializer.class, JacksonJodaDateFormat.class })
	static class JodaDateTimeJacksonConfiguration {

		private final Log log = LogFactory.getLog(JodaDateTimeJacksonConfiguration.class);

		@Autowired
		private JacksonProperties jacksonProperties;

		@Bean
		public Module jodaDateTimeSerializationModule() {
			SimpleModule module = new SimpleModule();
			JacksonJodaDateFormat jacksonJodaFormat = getJacksonJodaDateFormat();
			if (jacksonJodaFormat != null) {
				module.addSerializer(DateTime.class, new DateTimeSerializer(
						jacksonJodaFormat));
			}
			return module;
		}

		private JacksonJodaDateFormat getJacksonJodaDateFormat() {
			if (this.jacksonProperties.getJodaDateTimeFormat() != null) {
				return new JacksonJodaDateFormat(DateTimeFormat.forPattern(
						this.jacksonProperties.getJodaDateTimeFormat()).withZoneUTC());
			}
			if (this.jacksonProperties.getDateFormat() != null) {
				try {
					return new JacksonJodaDateFormat(DateTimeFormat.forPattern(
							this.jacksonProperties.getDateFormat()).withZoneUTC());
				}
				catch (IllegalArgumentException ex) {
					if (this.log.isWarnEnabled()) {
						this.log.warn("spring.jackson.date-format could not be used to "
								+ "configure formatting of Joda's DateTime. You may want "
								+ "to configure spring.jackson.joda-date-time-format as "
								+ "well.");
					}
				}
			}
			return null;
		}

	}

	@Configuration
	@ConditionalOnClass({ ObjectMapper.class, Jackson2ObjectMapperBuilder.class })
	@EnableConfigurationProperties(JacksonProperties.class)
	static class JacksonObjectMapperBuilderConfiguration implements
			ApplicationContextAware {

		private ApplicationContext applicationContext;

		@Autowired
		private JacksonProperties jacksonProperties;

		@Bean
		@ConditionalOnMissingBean(Jackson2ObjectMapperBuilder.class)
		public Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder() {
			Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
			builder.applicationContext(this.applicationContext);
			if (this.jacksonProperties.getSerializationInclusion() != null) {
				builder.serializationInclusion(this.jacksonProperties
						.getSerializationInclusion());
			}
			configureFeatures(builder, this.jacksonProperties.getDeserialization());
			configureFeatures(builder, this.jacksonProperties.getSerialization());
			configureFeatures(builder, this.jacksonProperties.getMapper());
			configureFeatures(builder, this.jacksonProperties.getParser());
			configureFeatures(builder, this.jacksonProperties.getGenerator());
			configureDateFormat(builder);
			configurePropertyNamingStrategy(builder);
			configureModules(builder);
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

		private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
			// We support a fully qualified class name extending DateFormat or a date
			// pattern string value
			String dateFormat = this.jacksonProperties.getDateFormat();
			if (dateFormat != null) {
				try {
					Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
					builder.dateFormat((DateFormat) BeanUtils
							.instantiateClass(dateFormatClass));
				}
				catch (ClassNotFoundException ex) {
					builder.dateFormat(new SimpleDateFormat(dateFormat));
				}
			}
		}

		private void configurePropertyNamingStrategy(Jackson2ObjectMapperBuilder builder) {
			// We support a fully qualified class name extending Jackson's
			// PropertyNamingStrategy or a string value corresponding to the constant
			// names in PropertyNamingStrategy which hold default provided implementations
			String strategy = this.jacksonProperties.getPropertyNamingStrategy();
			if (strategy != null) {
				try {
					configurePropertyNamingStrategyClass(builder,
							ClassUtils.forName(strategy, null));
				}
				catch (ClassNotFoundException ex) {
					configurePropertyNamingStrategyField(builder, strategy);
				}
			}
		}

		private void configurePropertyNamingStrategyClass(
				Jackson2ObjectMapperBuilder builder, Class<?> propertyNamingStrategyClass) {
			builder.propertyNamingStrategy((PropertyNamingStrategy) BeanUtils
					.instantiateClass(propertyNamingStrategyClass));
		}

		private void configurePropertyNamingStrategyField(
				Jackson2ObjectMapperBuilder builder, String fieldName) {
			// Find the field (this way we automatically support new constants
			// that may be added by Jackson in the future)
			Field field = ReflectionUtils.findField(PropertyNamingStrategy.class,
					fieldName, PropertyNamingStrategy.class);
			Assert.notNull(field, "Constant named '" + fieldName + "' not found on "
					+ PropertyNamingStrategy.class.getName());
			try {
				builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		private void configureModules(Jackson2ObjectMapperBuilder builder) {
			Collection<Module> moduleBeans = getBeans(this.applicationContext,
					Module.class);
			builder.modulesToInstall(moduleBeans.toArray(new Module[moduleBeans.size()]));
		}

		private static <T> Collection<T> getBeans(ListableBeanFactory beanFactory,
				Class<T> type) {
			return BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, type)
					.values();
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}
	}

}
