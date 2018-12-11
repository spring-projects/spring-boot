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

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Configuration properties to configure Jackson.
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Johannes Edmeier
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.jackson")
public class JacksonProperties {

	private static final Map<?, Boolean> FEATURE_DEFAULTS;

	static {
		Map<Object, Boolean> featureDefaults = new HashMap<>();
		featureDefaults.put(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		FEATURE_DEFAULTS = Collections.unmodifiableMap(featureDefaults);
	}

	/**
	 * Date format string or a fully-qualified date format class name. For instance,
	 * `yyyy-MM-dd HH:mm:ss`.
	 */
	private String dateFormat;

	/**
	 * Joda date time format string. If not configured, "date-format" is used as a
	 * fallback if it is configured with a format string.
	 */
	private String jodaDateTimeFormat;

	/**
	 * One of the constants on Jackson's PropertyNamingStrategy. Can also be a
	 * fully-qualified class name of a PropertyNamingStrategy subclass.
	 */
	private String propertyNamingStrategy;

	/**
	 * Jackson visibility thresholds that can be used to limit which methods (and fields)
	 * are auto-detected.
	 */
	private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new EnumMap<>(
			PropertyAccessor.class);

	/**
	 * Jackson on/off features that affect the way Java objects are serialized.
	 */
	private final Map<SerializationFeature, Boolean> serialization = new EnumMap<>(
			SerializationFeature.class);

	/**
	 * Jackson on/off features that affect the way Java objects are deserialized.
	 */
	private final Map<DeserializationFeature, Boolean> deserialization = new EnumMap<>(
			DeserializationFeature.class);

	/**
	 * Jackson general purpose on/off features.
	 */
	private final Map<MapperFeature, Boolean> mapper = new EnumMap<>(MapperFeature.class);

	/**
	 * Jackson on/off features for parsers.
	 */
	private final Map<JsonParser.Feature, Boolean> parser = new EnumMap<>(
			JsonParser.Feature.class);

	/**
	 * Jackson on/off features for generators.
	 */
	private final Map<JsonGenerator.Feature, Boolean> generator = new EnumMap<>(
			JsonGenerator.Feature.class);

	/**
	 * Controls the inclusion of properties during serialization. Configured with one of
	 * the values in Jackson's JsonInclude.Include enumeration.
	 */
	private JsonInclude.Include defaultPropertyInclusion;

	/**
	 * Time zone used when formatting dates. For instance, "America/Los_Angeles" or
	 * "GMT+10".
	 */
	private TimeZone timeZone = null;

	/**
	 * Locale used for formatting.
	 */
	private Locale locale;

	public Jackson2ObjectMapperBuilder initializeJackson2ObjectMapperBuilder() {
		return initializeJackson2ObjectMapperBuilder(null);
	}

	public Jackson2ObjectMapperBuilder initializeJackson2ObjectMapperBuilder(
			ApplicationContext applicationContext) {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		if (this.getDefaultPropertyInclusion() != null) {
			builder.serializationInclusion(this.getDefaultPropertyInclusion());
		}
		if (this.getTimeZone() != null) {
			builder.timeZone(this.getTimeZone());
		}
		configureFeatures(builder, FEATURE_DEFAULTS);
		configureVisibility(builder, getVisibility());
		configureFeatures(builder, getDeserialization());
		configureFeatures(builder, getSerialization());
		configureFeatures(builder, getMapper());
		configureFeatures(builder, getParser());
		configureFeatures(builder, getGenerator());
		configureDateFormat(builder);
		configurePropertyNamingStrategy(builder);
		configureModules(builder, applicationContext);
		configureLocale(builder);
		return builder;
	}

	private void configureFeatures(Jackson2ObjectMapperBuilder builder,
			Map<?, Boolean> features) {
		features.forEach((feature, value) -> {
			if (value != null) {
				if (value) {
					builder.featuresToEnable(feature);
				}
				else {
					builder.featuresToDisable(feature);
				}
			}
		});
	}

	private void configureVisibility(Jackson2ObjectMapperBuilder builder,
			Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
		visibilities.forEach(builder::visibility);
	}

	private void configureDateFormat(Jackson2ObjectMapperBuilder builder) {
		// We support a fully qualified class name extending DateFormat or a date
		// pattern string value
		String dateFormat = getDateFormat();
		if (dateFormat != null) {
			try {
				Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
				builder.dateFormat(
						(DateFormat) BeanUtils.instantiateClass(dateFormatClass));
			}
			catch (ClassNotFoundException ex) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
				// Since Jackson 2.6.3 we always need to set a TimeZone (see
				// gh-4170). If none in our properties fallback to the Jackson's
				// default
				TimeZone timeZone = getTimeZone();
				if (timeZone == null) {
					timeZone = new ObjectMapper().getSerializationConfig().getTimeZone();
				}
				simpleDateFormat.setTimeZone(timeZone);
				builder.dateFormat(simpleDateFormat);
			}
		}
	}

	private void configurePropertyNamingStrategy(Jackson2ObjectMapperBuilder builder) {
		// We support a fully qualified class name extending Jackson's
		// PropertyNamingStrategy or a string value corresponding to the constant
		// names in PropertyNamingStrategy which hold default provided
		// implementations
		String strategy = getPropertyNamingStrategy();
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

	private void configurePropertyNamingStrategyClass(Jackson2ObjectMapperBuilder builder,
			Class<?> propertyNamingStrategyClass) {
		builder.propertyNamingStrategy((PropertyNamingStrategy) BeanUtils
				.instantiateClass(propertyNamingStrategyClass));
	}

	private void configurePropertyNamingStrategyField(Jackson2ObjectMapperBuilder builder,
			String fieldName) {
		// Find the field (this way we automatically support new constants
		// that may be added by Jackson in the future)
		Field field = ReflectionUtils.findField(PropertyNamingStrategy.class, fieldName,
				PropertyNamingStrategy.class);
		Assert.notNull(field, () -> "Constant named '" + fieldName + "' not found on "
				+ PropertyNamingStrategy.class.getName());
		try {
			builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void configureModules(Jackson2ObjectMapperBuilder builder,
			ApplicationContext applicationContext) {
		Collection<Module> moduleBeans = getBeans(applicationContext, Module.class);
		builder.modulesToInstall(moduleBeans.toArray(new Module[0]));
	}

	private void configureLocale(Jackson2ObjectMapperBuilder builder) {
		Locale locale = getLocale();
		if (locale != null) {
			builder.locale(locale);
		}
	}

	private static <T> Collection<T> getBeans(ListableBeanFactory beanFactory,
			Class<T> type) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, type).values();
	}

	public String getDateFormat() {
		return this.dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String getJodaDateTimeFormat() {
		return this.jodaDateTimeFormat;
	}

	public void setJodaDateTimeFormat(String jodaDataTimeFormat) {
		this.jodaDateTimeFormat = jodaDataTimeFormat;
	}

	public String getPropertyNamingStrategy() {
		return this.propertyNamingStrategy;
	}

	public void setPropertyNamingStrategy(String propertyNamingStrategy) {
		this.propertyNamingStrategy = propertyNamingStrategy;
	}

	public Map<PropertyAccessor, JsonAutoDetect.Visibility> getVisibility() {
		return this.visibility;
	}

	public Map<SerializationFeature, Boolean> getSerialization() {
		return this.serialization;
	}

	public Map<DeserializationFeature, Boolean> getDeserialization() {
		return this.deserialization;
	}

	public Map<MapperFeature, Boolean> getMapper() {
		return this.mapper;
	}

	public Map<JsonParser.Feature, Boolean> getParser() {
		return this.parser;
	}

	public Map<JsonGenerator.Feature, Boolean> getGenerator() {
		return this.generator;
	}

	public JsonInclude.Include getDefaultPropertyInclusion() {
		return this.defaultPropertyInclusion;
	}

	public void setDefaultPropertyInclusion(
			JsonInclude.Include defaultPropertyInclusion) {
		this.defaultPropertyInclusion = defaultPropertyInclusion;
	}

	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}
