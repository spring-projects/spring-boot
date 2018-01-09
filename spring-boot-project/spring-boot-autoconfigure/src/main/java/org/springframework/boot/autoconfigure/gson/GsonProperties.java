/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.annotations.Expose;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure {@link Gson}.
 *
 * @author Ivan Golovko
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.gson")
public class GsonProperties {

	/**
	 * Makes the output JSON non-executable in Javascript by prefixing the generated JSON
	 * with some special text.
	 */
	private boolean generateNonExecutableJson;

	/**
	 * Configures {@link Gson} to exclude all fields from consideration for serialization
	 * or deserialization that do not have the {@link Expose} annotation.
	 */
	private boolean excludeFieldsWithoutExposeAnnotation;

	/**
	 * Configure {@link Gson} to serialize null fields.
	 */
	private boolean serializeNulls;

	/**
	 * Enabling this feature will only change the serialized form if the map key is a
	 * complex type (i.e. non-primitive) in its serialized JSON form
	 */
	private boolean enableComplexMapKeySerialization;

	/**
	 * Configures {@link Gson} to exclude inner classes during serialization.
	 */
	private boolean disableInnerClassSerialization;

	/**
	 * Configures {@link Gson} to apply a specific serialization policy for Long and long
	 * objects.
	 */
	private LongSerializationPolicy longSerializationPolicy;

	/**
	 * Configures {@link Gson} to apply a specific naming policy to an object's field
	 * during serialization and deserialization.
	 */
	private FieldNamingPolicy fieldNamingPolicy;

	/**
	 * Configures {@link Gson} to output Json that fits in a page for pretty printing.
	 * This option only affects Json serialization.
	 */
	private boolean prettyPrinting;

	/**
	 * By default, {@link Gson} is strict and only accepts JSON as specified by RFC 4627.
	 * This option makes the parser liberal in what it accepts.
	 */
	private boolean lenient;

	/**
	 * By default, {@link Gson} escapes HTML characters such as < > etc. Use this option
	 * to configure Gson to pass-through HTML characters as is.
	 */
	private boolean disableHtmlEscaping;

	/**
	 * Configures {@link Gson} to serialize Date objects according to the pattern
	 * provided.
	 */
	private String dateFormat;

	public boolean isGenerateNonExecutableJson() {
		return this.generateNonExecutableJson;
	}

	public void setGenerateNonExecutableJson(boolean generateNonExecutableJson) {
		this.generateNonExecutableJson = generateNonExecutableJson;
	}

	public boolean isExcludeFieldsWithoutExposeAnnotation() {
		return this.excludeFieldsWithoutExposeAnnotation;
	}

	public void setExcludeFieldsWithoutExposeAnnotation(
			boolean excludeFieldsWithoutExposeAnnotation) {
		this.excludeFieldsWithoutExposeAnnotation = excludeFieldsWithoutExposeAnnotation;
	}

	public boolean isSerializeNulls() {
		return this.serializeNulls;
	}

	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	public boolean isEnableComplexMapKeySerialization() {
		return this.enableComplexMapKeySerialization;
	}

	public void setEnableComplexMapKeySerialization(
			boolean enableComplexMapKeySerialization) {
		this.enableComplexMapKeySerialization = enableComplexMapKeySerialization;
	}

	public boolean isDisableInnerClassSerialization() {
		return this.disableInnerClassSerialization;
	}

	public void setDisableInnerClassSerialization(
			boolean disableInnerClassSerialization) {
		this.disableInnerClassSerialization = disableInnerClassSerialization;
	}

	public LongSerializationPolicy getLongSerializationPolicy() {
		return this.longSerializationPolicy;
	}

	public void setLongSerializationPolicy(
			LongSerializationPolicy longSerializationPolicy) {
		this.longSerializationPolicy = longSerializationPolicy;
	}

	public FieldNamingPolicy getFieldNamingPolicy() {
		return this.fieldNamingPolicy;
	}

	public void setFieldNamingPolicy(FieldNamingPolicy fieldNamingPolicy) {
		this.fieldNamingPolicy = fieldNamingPolicy;
	}

	public boolean isPrettyPrinting() {
		return this.prettyPrinting;
	}

	public void setPrettyPrinting(boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	public boolean isLenient() {
		return this.lenient;
	}

	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	public boolean isDisableHtmlEscaping() {
		return this.disableHtmlEscaping;
	}

	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	public String getDateFormat() {
		return this.dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
}
