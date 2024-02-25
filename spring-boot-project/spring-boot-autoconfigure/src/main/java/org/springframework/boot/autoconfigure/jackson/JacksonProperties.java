/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.jackson;

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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Jackson.
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Johannes Edmeier
 * @author Eddú Meléndez
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.jackson")
public class JacksonProperties {

	/**
	 * Date format string or a fully-qualified date format class name. For instance,
	 * 'yyyy-MM-dd HH:mm:ss'.
	 */
	private String dateFormat;

	/**
	 * One of the constants on Jackson's PropertyNamingStrategies. Can also be a
	 * fully-qualified class name of a PropertyNamingStrategy implementation.
	 */
	private String propertyNamingStrategy;

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
	private JsonInclude.Include defaultPropertyInclusion;

	/**
	 * Global default setting (if any) for leniency.
	 */
	private Boolean defaultLeniency;

	/**
	 * Strategy to use to auto-detect constructor, and in particular behavior with
	 * single-argument constructors.
	 */
	private ConstructorDetectorStrategy constructorDetector;

	/**
	 * Time zone used when formatting dates. For instance, "America/Los_Angeles" or
	 * "GMT+10".
	 */
	private TimeZone timeZone = null;

	/**
	 * Locale used for formatting.
	 */
	private Locale locale;

	private final Datatype datatype = new Datatype();

	/**
	 * Returns the date format used by the JacksonProperties class.
	 * @return the date format
	 */
	public String getDateFormat() {
		return this.dateFormat;
	}

	/**
	 * Sets the date format for the JacksonProperties class.
	 * @param dateFormat the desired date format to be set
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Returns the property naming strategy used by this JacksonProperties object.
	 * @return the property naming strategy
	 */
	public String getPropertyNamingStrategy() {
		return this.propertyNamingStrategy;
	}

	/**
	 * Sets the property naming strategy for the JacksonProperties class.
	 * @param propertyNamingStrategy the property naming strategy to be set
	 */
	public void setPropertyNamingStrategy(String propertyNamingStrategy) {
		this.propertyNamingStrategy = propertyNamingStrategy;
	}

	/**
	 * Returns the visibility settings for the property accessors.
	 * @return a map containing the visibility settings for the property accessors
	 */
	public Map<PropertyAccessor, JsonAutoDetect.Visibility> getVisibility() {
		return this.visibility;
	}

	/**
	 * Returns the serialization configuration settings.
	 * @return a map containing the serialization features and their corresponding boolean
	 * values
	 */
	public Map<SerializationFeature, Boolean> getSerialization() {
		return this.serialization;
	}

	/**
	 * Returns the map of deserialization features and their corresponding boolean values.
	 * @return the map of deserialization features and their corresponding boolean values
	 */
	public Map<DeserializationFeature, Boolean> getDeserialization() {
		return this.deserialization;
	}

	/**
	 * Returns the mapper configuration settings as a map.
	 * @return the mapper configuration settings as a map
	 */
	public Map<MapperFeature, Boolean> getMapper() {
		return this.mapper;
	}

	/**
	 * Returns the parser configuration settings as a map of JSON parser features and
	 * their corresponding boolean values.
	 * @return the map of JSON parser features and their corresponding boolean values
	 */
	public Map<JsonParser.Feature, Boolean> getParser() {
		return this.parser;
	}

	/**
	 * Returns the map of JSON generator features and their corresponding boolean values.
	 * @return the map of JSON generator features and their corresponding boolean values
	 */
	public Map<JsonGenerator.Feature, Boolean> getGenerator() {
		return this.generator;
	}

	/**
	 * Returns the default property inclusion setting for JSON serialization.
	 * @return the default property inclusion setting
	 */
	public JsonInclude.Include getDefaultPropertyInclusion() {
		return this.defaultPropertyInclusion;
	}

	/**
	 * Sets the default property inclusion for JSON serialization.
	 * @param defaultPropertyInclusion the default property inclusion to be set
	 */
	public void setDefaultPropertyInclusion(JsonInclude.Include defaultPropertyInclusion) {
		this.defaultPropertyInclusion = defaultPropertyInclusion;
	}

	/**
	 * Returns the default leniency value.
	 * @return the default leniency value
	 */
	public Boolean getDefaultLeniency() {
		return this.defaultLeniency;
	}

	/**
	 * Sets the default leniency for the JacksonProperties class.
	 * @param defaultLeniency the default leniency value to be set
	 */
	public void setDefaultLeniency(Boolean defaultLeniency) {
		this.defaultLeniency = defaultLeniency;
	}

	/**
	 * Returns the constructor detector strategy used by this JacksonProperties instance.
	 * @return the constructor detector strategy
	 */
	public ConstructorDetectorStrategy getConstructorDetector() {
		return this.constructorDetector;
	}

	/**
	 * Sets the constructor detector strategy for the JacksonProperties class.
	 * @param constructorDetector the constructor detector strategy to be set
	 */
	public void setConstructorDetector(ConstructorDetectorStrategy constructorDetector) {
		this.constructorDetector = constructorDetector;
	}

	/**
	 * Returns the time zone associated with this JacksonProperties object.
	 * @return the time zone
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * Sets the time zone for the JacksonProperties class.
	 * @param timeZone the time zone to be set
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Returns the locale of the JacksonProperties object.
	 * @return the locale of the JacksonProperties object
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Sets the locale for the JacksonProperties class.
	 * @param locale the locale to be set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Returns the datatype of the JacksonProperties object.
	 * @return the datatype of the JacksonProperties object
	 */
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

	/**
	 * Datatype class.
	 */
	public static class Datatype {

		/**
		 * Jackson on/off features for enums.
		 */
		private final Map<EnumFeature, Boolean> enumFeatures = new EnumMap<>(EnumFeature.class);

		/**
		 * Jackson on/off features for JsonNodes.
		 */
		private final Map<JsonNodeFeature, Boolean> jsonNode = new EnumMap<>(JsonNodeFeature.class);

		/**
		 * Returns a map of enum features and their corresponding boolean values.
		 * @return a map of enum features and their corresponding boolean values
		 */
		public Map<EnumFeature, Boolean> getEnum() {
			return this.enumFeatures;
		}

		/**
		 * Retrieves the JSON node map.
		 * @return the JSON node map
		 */
		public Map<JsonNodeFeature, Boolean> getJsonNode() {
			return this.jsonNode;
		}

	}

}
