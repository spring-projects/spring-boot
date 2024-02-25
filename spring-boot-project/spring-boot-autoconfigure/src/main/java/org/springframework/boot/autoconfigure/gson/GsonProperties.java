/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.gson;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.LongSerializationPolicy;

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
	 * Whether to generate non-executable JSON by prefixing the output with some special
	 * text.
	 */
	private Boolean generateNonExecutableJson;

	/**
	 * Whether to exclude all fields from consideration for serialization or
	 * deserialization that do not have the "Expose" annotation.
	 */
	private Boolean excludeFieldsWithoutExposeAnnotation;

	/**
	 * Whether to serialize null fields.
	 */
	private Boolean serializeNulls;

	/**
	 * Whether to enable serialization of complex map keys (i.e. non-primitives).
	 */
	private Boolean enableComplexMapKeySerialization;

	/**
	 * Whether to exclude inner classes during serialization.
	 */
	private Boolean disableInnerClassSerialization;

	/**
	 * Serialization policy for Long and long types.
	 */
	private LongSerializationPolicy longSerializationPolicy;

	/**
	 * Naming policy that should be applied to an object's field during serialization and
	 * deserialization.
	 */
	private FieldNamingPolicy fieldNamingPolicy;

	/**
	 * Whether to output serialized JSON that fits in a page for pretty printing.
	 */
	private Boolean prettyPrinting;

	/**
	 * Whether to be lenient about parsing JSON that doesn't conform to RFC 4627.
	 */
	private Boolean lenient;

	/**
	 * Whether to disable the escaping of HTML characters such as '<', '>', etc.
	 */
	private Boolean disableHtmlEscaping;

	/**
	 * Format to use when serializing Date objects.
	 */
	private String dateFormat;

	/**
     * Returns the value of the generateNonExecutableJson property.
     * 
     * @return true if non-executable JSON should be generated, false otherwise
     */
    public Boolean getGenerateNonExecutableJson() {
		return this.generateNonExecutableJson;
	}

	/**
     * Sets the flag to generate non-executable JSON.
     * 
     * @param generateNonExecutableJson the flag indicating whether to generate non-executable JSON
     */
    public void setGenerateNonExecutableJson(Boolean generateNonExecutableJson) {
		this.generateNonExecutableJson = generateNonExecutableJson;
	}

	/**
     * Returns the value of the excludeFieldsWithoutExposeAnnotation property.
     * 
     * @return the value of the excludeFieldsWithoutExposeAnnotation property
     */
    public Boolean getExcludeFieldsWithoutExposeAnnotation() {
		return this.excludeFieldsWithoutExposeAnnotation;
	}

	/**
     * Sets the flag to exclude fields without the @Expose annotation.
     * 
     * @param excludeFieldsWithoutExposeAnnotation the flag indicating whether to exclude fields without the @Expose annotation
     */
    public void setExcludeFieldsWithoutExposeAnnotation(Boolean excludeFieldsWithoutExposeAnnotation) {
		this.excludeFieldsWithoutExposeAnnotation = excludeFieldsWithoutExposeAnnotation;
	}

	/**
     * Returns the value of the serializeNulls property.
     * 
     * @return the value of the serializeNulls property
     */
    public Boolean getSerializeNulls() {
		return this.serializeNulls;
	}

	/**
     * Sets the flag indicating whether null values should be serialized.
     * 
     * @param serializeNulls the flag indicating whether null values should be serialized
     */
    public void setSerializeNulls(Boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
     * Returns the value of the enableComplexMapKeySerialization property.
     * 
     * @return the value of the enableComplexMapKeySerialization property
     */
    public Boolean getEnableComplexMapKeySerialization() {
		return this.enableComplexMapKeySerialization;
	}

	/**
     * Sets the flag to enable or disable complex map key serialization.
     * 
     * @param enableComplexMapKeySerialization the flag indicating whether complex map key serialization is enabled or disabled
     */
    public void setEnableComplexMapKeySerialization(Boolean enableComplexMapKeySerialization) {
		this.enableComplexMapKeySerialization = enableComplexMapKeySerialization;
	}

	/**
     * Returns the value of the disableInnerClassSerialization property.
     * 
     * @return the value of the disableInnerClassSerialization property
     */
    public Boolean getDisableInnerClassSerialization() {
		return this.disableInnerClassSerialization;
	}

	/**
     * Sets the flag to disable inner class serialization.
     * 
     * @param disableInnerClassSerialization the flag indicating whether inner class serialization should be disabled
     */
    public void setDisableInnerClassSerialization(Boolean disableInnerClassSerialization) {
		this.disableInnerClassSerialization = disableInnerClassSerialization;
	}

	/**
     * Returns the long serialization policy of the GsonProperties object.
     * 
     * @return the long serialization policy
     */
    public LongSerializationPolicy getLongSerializationPolicy() {
		return this.longSerializationPolicy;
	}

	/**
     * Sets the long serialization policy for GsonProperties.
     * 
     * @param longSerializationPolicy the long serialization policy to be set
     */
    public void setLongSerializationPolicy(LongSerializationPolicy longSerializationPolicy) {
		this.longSerializationPolicy = longSerializationPolicy;
	}

	/**
     * Returns the field naming policy used by the GsonProperties class.
     * 
     * @return the field naming policy
     */
    public FieldNamingPolicy getFieldNamingPolicy() {
		return this.fieldNamingPolicy;
	}

	/**
     * Sets the field naming policy for the GsonProperties class.
     * 
     * @param fieldNamingPolicy the field naming policy to be set
     */
    public void setFieldNamingPolicy(FieldNamingPolicy fieldNamingPolicy) {
		this.fieldNamingPolicy = fieldNamingPolicy;
	}

	/**
     * Returns the value of the prettyPrinting property.
     * 
     * @return the value of the prettyPrinting property
     */
    public Boolean getPrettyPrinting() {
		return this.prettyPrinting;
	}

	/**
     * Sets the flag for pretty printing.
     * 
     * @param prettyPrinting the flag indicating whether pretty printing should be enabled or not
     */
    public void setPrettyPrinting(Boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	/**
     * Returns the value of the lenient property.
     *
     * @return the value of the lenient property
     */
    public Boolean getLenient() {
		return this.lenient;
	}

	/**
     * Sets the lenient flag for Gson.
     * 
     * @param lenient the lenient flag to be set
     */
    public void setLenient(Boolean lenient) {
		this.lenient = lenient;
	}

	/**
     * Returns the value of the disableHtmlEscaping property.
     *
     * @return the value of the disableHtmlEscaping property
     */
    public Boolean getDisableHtmlEscaping() {
		return this.disableHtmlEscaping;
	}

	/**
     * Sets the flag to disable HTML escaping.
     * 
     * @param disableHtmlEscaping the flag indicating whether HTML escaping should be disabled
     */
    public void setDisableHtmlEscaping(Boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	/**
     * Returns the date format used by the GsonProperties class.
     *
     * @return the date format
     */
    public String getDateFormat() {
		return this.dateFormat;
	}

	/**
     * Sets the date format for Gson serialization and deserialization.
     * 
     * @param dateFormat the date format to be set
     */
    public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

}
