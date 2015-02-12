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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Configuration properties to configure Jackson
 *
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.jackson")
public class JacksonProperties {

	/**
	 * Date format string (yyyy-MM-dd HH:mm:ss), or a fully-qualified date format class
	 * name.
	 */
	private String dateFormat;

	/**
	 * Joda date time format string (yyyy-MM-dd HH:mm:ss). If not configured,
	 * {@code date-format} will be used as a fallback if it is configured with a format
	 * string.
	 */
	private String jodaDateTimeFormat;

	/**
	 * One of the constants on Jackson's PropertyNamingStrategy
	 * (CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES). Can also be a fully-qualified class
	 * name of a PropertyNamingStrategy subclass.
	 */
	private String propertyNamingStrategy;

	/**
	 * Jackson on/off features that affect the way Java objects are serialized.
	 */
	private Map<SerializationFeature, Boolean> serialization = new HashMap<SerializationFeature, Boolean>();

	/**
	 * Jackson on/off features that affect the way Java objects are deserialized.
	 */
	private Map<DeserializationFeature, Boolean> deserialization = new HashMap<DeserializationFeature, Boolean>();

	/**
	 * Jackson general purpose on/off features.
	 */
	private Map<MapperFeature, Boolean> mapper = new HashMap<MapperFeature, Boolean>();

	/**
	 * Jackson on/off features for parsers.
	 */
	private Map<JsonParser.Feature, Boolean> parser = new HashMap<JsonParser.Feature, Boolean>();

	/**
	 * Jackson on/off features for generators.
	 */
	private Map<JsonGenerator.Feature, Boolean> generator = new HashMap<JsonGenerator.Feature, Boolean>();

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

}
