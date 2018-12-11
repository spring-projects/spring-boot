/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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
 * @author Johannes Edmeier
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonAutoConfiguration {

	private static final Map<?, Boolean> FEATURE_DEFAULTS;

	static {
		Map<Object, Boolean> featureDefaults = new HashMap<>();
		featureDefaults.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		FEATURE_DEFAULTS = Collections.unmodifiableMap(featureDefaults);
	}

	@Bean
	public JsonComponentModule jsonComponentModule() {
		return new JsonComponentModule();
	}

	@Configuration
	@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
	static class JacksonObjectMapperConfiguration {

		@Bean
		@Primary
		@ConditionalOnMissingBean
		public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
			return builder.createXmlMapper(false).build();
		}

	}

	@Configuration
	@ConditionalOnClass({ Jackson2ObjectMapperBuilder.class, DateTime.class,
			DateTimeSerializer.class, JacksonJodaDateFormat.class })
	static class JodaDateTimeJacksonConfiguration {

		private static final Log logger = LogFactory
				.getLog(JodaDateTimeJacksonConfiguration.class);

		private final JacksonProperties jacksonProperties;

		JodaDateTimeJacksonConfiguration(JacksonProperties jacksonProperties) {
			this.jacksonProperties = jacksonProperties;
		}

		@Bean
		public SimpleModule jodaDateTimeSerializationModule() {
			SimpleModule module = new SimpleModule();
			JacksonJodaDateFormat jacksonJodaFormat = getJacksonJodaDateFormat();
			if (jacksonJodaFormat != null) {
				module.addSerializer(DateTime.class,
						new DateTimeSerializer(jacksonJodaFormat, 0));
			}
			return module;
		}

		private JacksonJodaDateFormat getJacksonJodaDateFormat() {
			if (this.jacksonProperties.getJodaDateTimeFormat() != null) {
				return new JacksonJodaDateFormat(DateTimeFormat
						.forPattern(this.jacksonProperties.getJodaDateTimeFormat())
						.withZoneUTC());
			}
			if (this.jacksonProperties.getDateFormat() != null) {
				try {
					return new JacksonJodaDateFormat(DateTimeFormat
							.forPattern(this.jacksonProperties.getDateFormat())
							.withZoneUTC());
				}
				catch (IllegalArgumentException ex) {
					if (logger.isWarnEnabled()) {
						logger.warn("spring.jackson.date-format could not be used to "
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
	@ConditionalOnClass(ParameterNamesModule.class)
	static class ParameterNamesModuleConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ParameterNamesModule parameterNamesModule() {
			return new ParameterNamesModule(JsonCreator.Mode.DEFAULT);
		}

	}

	@Configuration
	@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
	static class JacksonObjectMapperBuilderConfiguration {

		private final ApplicationContext applicationContext;

		JacksonObjectMapperBuilderConfiguration(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		@ConditionalOnMissingBean
		public Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder(
				List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
			Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
			builder.applicationContext(this.applicationContext);
			customize(builder, customizers);
			return builder;
		}

		private void customize(Jackson2ObjectMapperBuilder builder,
				List<Jackson2ObjectMapperBuilderCustomizer> customizers) {
			for (Jackson2ObjectMapperBuilderCustomizer customizer : customizers) {
				customizer.customize(builder);
			}
		}

	}

	@Configuration
	@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
	static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

		@ConfigurationProperties(prefix = "spring.jackson")
		@Bean
		public JacksonProperties jacksonProperties() {
			return new JacksonProperties();
		}

		@Bean
		public StandardJackson2ObjectMapperBuilderCustomizer standardJacksonObjectMapperBuilderCustomizer(
				ApplicationContext applicationContext,
				JacksonProperties jacksonProperties) {
			return new StandardJackson2ObjectMapperBuilderCustomizer(applicationContext,
					jacksonProperties);
		}

		static final class StandardJackson2ObjectMapperBuilderCustomizer
				implements Jackson2ObjectMapperBuilderCustomizer, Ordered {

			private final ApplicationContext applicationContext;

			private final JacksonProperties jacksonProperties;

			StandardJackson2ObjectMapperBuilderCustomizer(
					ApplicationContext applicationContext,
					JacksonProperties jacksonProperties) {
				this.applicationContext = applicationContext;
				this.jacksonProperties = jacksonProperties;
			}

			@Override
			public int getOrder() {
				return 0;
			}

			@Override
			public void customize(Jackson2ObjectMapperBuilder builder) {
				this.jacksonProperties
						.initializeJackson2ObjectMapperBuilder(this.applicationContext);
			}

		}

	}

}
