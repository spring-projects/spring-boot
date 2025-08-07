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

package org.springframework.boot.jackson.autoconfigure;

import java.util.EnumMap;
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Jackson.
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@ConfigurationProperties("spring.jackson")
public class JacksonProperties {

	/**
	 * Date format string or a fully-qualified date format class name. For instance,
	 * 'yyyy-MM-dd HH:mm:ss'.
	 */
	private @Nullable String dateFormat;

	/**
	 * One of the constants on Jackson's PropertyNamingStrategies. Can also be a
	 * fully-qualified class name of a PropertyNamingStrategy implementation.
	 */
	private @Nullable String propertyNamingStrategy;

	/**
	 * Jackson visibility thresholds that can be used to limit which methods (and fields)
	 * are auto-detected.
	 */
	private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibility = new EnumMap<>(PropertyAccessor.class);

	/**
	 * Jackson on/off features that affect the way Java objects are serialized.
	 */
	private final Map<SerializationFeature, Boolean> serialization = new EnumMap<>(SerializationFeature.class);

	/**
	 * Jackson on/off features that affect the way Java objects are deserialized.
	 */
	private final Map<DeserializationFeature, Boolean> deserialization = new EnumMap<>(DeserializationFeature.class);

	/**
	 * Jackson general purpose on/off features.
	 */
	private final Map<MapperFeature, Boolean> mapper = new EnumMap<>(MapperFeature.class);

	/**
	 * Jackson on/off features for parsers.
	 */
	private final Map<JsonParser.Feature, Boolean> parser = new EnumMap<>(JsonParser.Feature.class);

	/**
	 * Jackson on/off features for generators.
	 */
	private final Map<JsonGenerator.Feature, Boolean> generator = new EnumMap<>(JsonGenerator.Feature.class);

	/**
	 * Controls the inclusion of properties during serialization. Configured with one of
	 * the values in Jackson's JsonInclude.Include enumeration.
	 */
	private JsonInclude.@Nullable Include defaultPropertyInclusion;

	/**
	 * Global default setting (if any) for leniency.
	 */
	private @Nullable Boolean defaultLeniency;

	/**
	 * Strategy to use to auto-detect constructor, and in particular behavior with
	 * single-argument constructors.
	 */
	private @Nullable ConstructorDetectorStrategy constructorDetector;

	/**
	 * Time zone used when formatting dates. For instance, "America/Los_Angeles" or
	 * "GMT+10".
	 */
	private @Nullable TimeZone timeZone;

	/**
	 * Locale used for formatting.
	 */
	private @Nullable Locale locale;

	private final Datatype datatype = new Datatype();

	public @Nullable String getDateFormat() {
		return this.dateFormat;
	}

	public void setDateFormat(@Nullable String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public @Nullable String getPropertyNamingStrategy() {
		return this.propertyNamingStrategy;
	}

	public void setPropertyNamingStrategy(@Nullable String propertyNamingStrategy) {
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

	public JsonInclude.@Nullable Include getDefaultPropertyInclusion() {
		return this.defaultPropertyInclusion;
	}

	public void setDefaultPropertyInclusion(JsonInclude.@Nullable Include defaultPropertyInclusion) {
		this.defaultPropertyInclusion = defaultPropertyInclusion;
	}

	public @Nullable Boolean getDefaultLeniency() {
		return this.defaultLeniency;
	}

	public void setDefaultLeniency(@Nullable Boolean defaultLeniency) {
		this.defaultLeniency = defaultLeniency;
	}

	public @Nullable ConstructorDetectorStrategy getConstructorDetector() {
		return this.constructorDetector;
	}

	public void setConstructorDetector(@Nullable ConstructorDetectorStrategy constructorDetector) {
		this.constructorDetector = constructorDetector;
	}

	public @Nullable TimeZone getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(@Nullable TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public @Nullable Locale getLocale() {
		return this.locale;
	}

	public void setLocale(@Nullable Locale locale) {
		this.locale = locale;
	}

	public Datatype getDatatype() {
		return this.datatype;
	}

	public enum ConstructorDetectorStrategy {

		/**
		 * Use heuristics to see if "properties" mode is to be used.
		 */
		DEFAULT,

		/**
		 * Assume "properties" mode if not explicitly annotated otherwise.
		 */
		USE_PROPERTIES_BASED,

		/**
		 * Assume "delegating" mode if not explicitly annotated otherwise.
		 */
		USE_DELEGATING,

		/**
		 * Refuse to decide implicit mode and instead throw an InvalidDefinitionException
		 * for ambiguous cases.
		 */
		EXPLICIT_ONLY

	}

	public static class Datatype {

		/**
		 * Jackson on/off features for enums.
		 */
		private final Map<EnumFeature, Boolean> enumFeatures = new EnumMap<>(EnumFeature.class);

		/**
		 * Jackson on/off features for JsonNodes.
		 */
		private final Map<JsonNodeFeature, Boolean> jsonNode = new EnumMap<>(JsonNodeFeature.class);

		public Map<EnumFeature, Boolean> getEnum() {
			return this.enumFeatures;
		}

		public Map<JsonNodeFeature, Boolean> getJsonNode() {
			return this.jsonNode;
		}

	}

}
