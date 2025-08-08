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

package org.springframework.boot.kotlin.serialization.autoconfigure;

import kotlinx.serialization.json.ClassDiscriminatorMode;
import kotlinx.serialization.json.Json;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Kotlin Serialization {@link Json}.
 *
 * @author Dmitry Sulman
 * @since 4.0.0
 */
@ConfigurationProperties("spring.kotlin-serialization")
public class KotlinSerializationProperties {

	/**
	 * Specifies JsonNamingStrategy that should be used for all properties in classes for
	 * serialization and deserialization.
	 */
	private JsonNamingStrategy namingStrategy;

	/**
	 * Whether resulting JSON should be pretty-printed: formatted and optimized for human
	 * readability.
	 */
	private Boolean prettyPrint;

	/**
	 * Enable lenient mode that removes JSON specification restriction (RFC-4627) and
	 * makes parser more liberal to the malformed input.
	 */
	private Boolean lenient;

	/**
	 * Whether encounters of unknown properties in the input JSON should be ignored
	 * instead of throwing SerializationException.
	 */
	private Boolean ignoreUnknownKeys;

	/**
	 * Whether default values of Kotlin properties should be encoded.
	 */
	private Boolean encodeDefaults;

	/**
	 * Whether null values should be encoded for nullable properties and must be present
	 * in JSON object during decoding.
	 */
	private Boolean explicitNulls;

	/**
	 * Enable coercing incorrect JSON values.
	 */
	private Boolean coerceInputValues;

	/**
	 * Enable structured objects to be serialized as map keys by changing serialized form
	 * of the map from JSON object (key-value pairs) to flat array like [k1, v1, k2, v2].
	 */
	private Boolean allowStructuredMapKeys;

	/**
	 * Whether to remove JSON specification restriction on special floating-point values
	 * such as 'NaN' and 'Infinity' and enable their serialization and deserialization as
	 * float literals without quotes.
	 */
	private Boolean allowSpecialFloatingPointValues;

	/**
	 * Name of the class descriptor property for polymorphic serialization.
	 */
	private String classDiscriminator;

	/**
	 * Defines which classes and objects should have class discriminator added to the
	 * output.
	 */
	private ClassDiscriminatorMode classDiscriminatorMode;

	/**
	 * Enable decoding enum values in a case-insensitive manner.
	 */
	private Boolean decodeEnumsCaseInsensitive;

	/**
	 * Whether Json instance makes use of JsonNames annotation.
	 */
	private Boolean useAlternativeNames;

	/**
	 * Whether to allow parser to accept trailing (ending) commas in JSON objects and
	 * arrays.
	 */
	private Boolean allowTrailingComma;

	/**
	 * Whether to allow parser to accept C/Java-style comments in JSON input.
	 */
	private Boolean allowComments;

	public JsonNamingStrategy getNamingStrategy() {
		return this.namingStrategy;
	}

	public void setNamingStrategy(JsonNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public Boolean getPrettyPrint() {
		return this.prettyPrint;
	}

	public void setPrettyPrint(Boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	public Boolean getLenient() {
		return this.lenient;
	}

	public void setLenient(Boolean lenient) {
		this.lenient = lenient;
	}

	public Boolean getIgnoreUnknownKeys() {
		return this.ignoreUnknownKeys;
	}

	public void setIgnoreUnknownKeys(Boolean ignoreUnknownKeys) {
		this.ignoreUnknownKeys = ignoreUnknownKeys;
	}

	public Boolean getEncodeDefaults() {
		return this.encodeDefaults;
	}

	public void setEncodeDefaults(Boolean encodeDefaults) {
		this.encodeDefaults = encodeDefaults;
	}

	public Boolean getExplicitNulls() {
		return this.explicitNulls;
	}

	public void setExplicitNulls(Boolean explicitNulls) {
		this.explicitNulls = explicitNulls;
	}

	public Boolean getCoerceInputValues() {
		return this.coerceInputValues;
	}

	public void setCoerceInputValues(Boolean coerceInputValues) {
		this.coerceInputValues = coerceInputValues;
	}

	public Boolean getAllowStructuredMapKeys() {
		return this.allowStructuredMapKeys;
	}

	public void setAllowStructuredMapKeys(Boolean allowStructuredMapKeys) {
		this.allowStructuredMapKeys = allowStructuredMapKeys;
	}

	public Boolean getAllowSpecialFloatingPointValues() {
		return this.allowSpecialFloatingPointValues;
	}

	public void setAllowSpecialFloatingPointValues(Boolean allowSpecialFloatingPointValues) {
		this.allowSpecialFloatingPointValues = allowSpecialFloatingPointValues;
	}

	public String getClassDiscriminator() {
		return this.classDiscriminator;
	}

	public void setClassDiscriminator(String classDiscriminator) {
		this.classDiscriminator = classDiscriminator;
	}

	public ClassDiscriminatorMode getClassDiscriminatorMode() {
		return this.classDiscriminatorMode;
	}

	public void setClassDiscriminatorMode(ClassDiscriminatorMode classDiscriminatorMode) {
		this.classDiscriminatorMode = classDiscriminatorMode;
	}

	public Boolean getDecodeEnumsCaseInsensitive() {
		return this.decodeEnumsCaseInsensitive;
	}

	public void setDecodeEnumsCaseInsensitive(Boolean decodeEnumsCaseInsensitive) {
		this.decodeEnumsCaseInsensitive = decodeEnumsCaseInsensitive;
	}

	public Boolean getUseAlternativeNames() {
		return this.useAlternativeNames;
	}

	public void setUseAlternativeNames(Boolean useAlternativeNames) {
		this.useAlternativeNames = useAlternativeNames;
	}

	public Boolean getAllowTrailingComma() {
		return this.allowTrailingComma;
	}

	public void setAllowTrailingComma(Boolean allowTrailingComma) {
		this.allowTrailingComma = allowTrailingComma;
	}

	public Boolean getAllowComments() {
		return this.allowComments;
	}

	public void setAllowComments(Boolean allowComments) {
		this.allowComments = allowComments;
	}

	/**
	 * Enum representing strategies for JSON property naming. The values correspond to
	 * {@link kotlinx.serialization.json.JsonNamingStrategy} implementations that cannot
	 * be directly referenced.
	 */
	public enum JsonNamingStrategy {

		/**
		 * Snake case strategy.
		 */
		SNAKE_CASE,

		/**
		 * Kebab case strategy.
		 */
		KEBAB_CASE

	}

}
