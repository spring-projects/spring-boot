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
 */
@ConfigurationProperties(prefix = "spring.jackson")
public class JacksonProperties {

	private Map<SerializationFeature, Boolean> serialization = new HashMap<SerializationFeature, Boolean>();

	private Map<DeserializationFeature, Boolean> deserialization = new HashMap<DeserializationFeature, Boolean>();

	private Map<MapperFeature, Boolean> mapper = new HashMap<MapperFeature, Boolean>();

	private Map<JsonParser.Feature, Boolean> parser = new HashMap<JsonParser.Feature, Boolean>();

	private Map<JsonGenerator.Feature, Boolean> generator = new HashMap<JsonGenerator.Feature, Boolean>();

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
