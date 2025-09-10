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
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Kotlin Serialization {@link Json}.
 *
 * @author Dmitry Sulman
 * @since 4.0.0
 */
@ConfigurationProperties("spring.kotlin.serialization")
public class KotlinSerializationProperties {

	/**
	 * Specifies JsonNamingStrategy that should be used for all properties in classes for
	 * serialization and deserialization.
	 */
	private @Nullable JsonNamingStrategy namingStrategy;

	/**
	 * Whether resulting JSON should be pretty-printed: formatted and optimized for human
	 * readability.
	 */
	private @Nullable Boolean prettyPrint;

	/**
	 * Enable lenient mode that removes JSON specification restriction (RFC-4627) and
	 * makes parser more liberal to the malformed input.
	 */
	private @Nullable Boolean lenient;

	/**
	 * Whether encounters of unknown properties in the input JSON should be ignored
	 * instead of throwing SerializationException.
	 */
	private @Nullable Boolean ignoreUnknownKeys;

	/**
	 * Whether default values of Kotlin properties should be encoded.
	 */
	private @Nullable Boolean encodeDefaults;

	/**
	 * Whether null values should be encoded for nullable properties and must be present
	 * in JSON object during decoding.
	 */
	private @Nullable Boolean explicitNulls;

	/**
	 * Enable coercing incorrect JSON values.
	 */
	private @Nullable Boolean coerceInputValues;

	/**
	 * Enable structured objects to be serialized as map keys by changing serialized form
	 * of the map from JSON object (key-value pairs) to flat array like [k1, v1, k2, v2].
	 */
	private @Nullable Boolean allowStructuredMapKeys;

	/**
	 * Whether to remove JSON specification restriction on special floating-point values
	 * such as 'NaN' and 'Infinity' and enable their serialization and deserialization as
	 * float literals without quotes.
	 */
	private @Nullable Boolean allowSpecialFloatingPointValues;

	/**
	 * Name of the class descriptor property for polymorphic serialization.
	 */
	private @Nullable String classDiscriminator;

	/**
	 * Defines which classes and objects should have class discriminator added to the
	 * output.
	 */
	private @Nullable ClassDiscriminatorMode classDiscriminatorMode;

	/**
	 * Enable decoding enum values in a case-insensitive manner.
	 */
	private @Nullable Boolean decodeEnumsCaseInsensitive;

	/**
	 * Whether Json instance makes use of JsonNames annotation.
	 */
	private @Nullable Boolean useAlternativeNames;

	/**
	 * Whether to allow parser to accept trailing (ending) commas in JSON objects and
	 * arrays.
	 */
	private @Nullable Boolean allowTrailingComma;

	/**
	 * Whether to allow parser to accept C/Java-style comments in JSON input.
	 */
	private @Nullable Boolean allowComments;

	public @Nullable JsonNamingStrategy getNamingStrategy() {
		return this.namingStrategy;
	}

	public void setNamingStrategy(@Nullable JsonNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public @Nullable Boolean getPrettyPrint() {
		return this.prettyPrint;
	}

	public void setPrettyPrint(@Nullable Boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	public @Nullable Boolean getLenient() {
		return this.lenient;
	}

	public void setLenient(@Nullable Boolean lenient) {
		this.lenient = lenient;
	}

	public @Nullable Boolean getIgnoreUnknownKeys() {
		return this.ignoreUnknownKeys;
	}

	public void setIgnoreUnknownKeys(@Nullable Boolean ignoreUnknownKeys) {
		this.ignoreUnknownKeys = ignoreUnknownKeys;
	}

	public @Nullable Boolean getEncodeDefaults() {
		return this.encodeDefaults;
	}

	public void setEncodeDefaults(@Nullable Boolean encodeDefaults) {
		this.encodeDefaults = encodeDefaults;
	}

	public @Nullable Boolean getExplicitNulls() {
		return this.explicitNulls;
	}

	public void setExplicitNulls(@Nullable Boolean explicitNulls) {
		this.explicitNulls = explicitNulls;
	}

	public @Nullable Boolean getCoerceInputValues() {
		return this.coerceInputValues;
	}

	public void setCoerceInputValues(@Nullable Boolean coerceInputValues) {
		this.coerceInputValues = coerceInputValues;
	}

	public @Nullable Boolean getAllowStructuredMapKeys() {
		return this.allowStructuredMapKeys;
	}

	public void setAllowStructuredMapKeys(@Nullable Boolean allowStructuredMapKeys) {
		this.allowStructuredMapKeys = allowStructuredMapKeys;
	}

	public @Nullable Boolean getAllowSpecialFloatingPointValues() {
		return this.allowSpecialFloatingPointValues;
	}

	public void setAllowSpecialFloatingPointValues(@Nullable Boolean allowSpecialFloatingPointValues) {
		this.allowSpecialFloatingPointValues = allowSpecialFloatingPointValues;
	}

	public @Nullable String getClassDiscriminator() {
		return this.classDiscriminator;
	}

	public void setClassDiscriminator(@Nullable String classDiscriminator) {
		this.classDiscriminator = classDiscriminator;
	}

	public @Nullable ClassDiscriminatorMode getClassDiscriminatorMode() {
		return this.classDiscriminatorMode;
	}

	public void setClassDiscriminatorMode(@Nullable ClassDiscriminatorMode classDiscriminatorMode) {
		this.classDiscriminatorMode = classDiscriminatorMode;
	}

	public @Nullable Boolean getDecodeEnumsCaseInsensitive() {
		return this.decodeEnumsCaseInsensitive;
	}

	public void setDecodeEnumsCaseInsensitive(@Nullable Boolean decodeEnumsCaseInsensitive) {
		this.decodeEnumsCaseInsensitive = decodeEnumsCaseInsensitive;
	}

	public @Nullable Boolean getUseAlternativeNames() {
		return this.useAlternativeNames;
	}

	public void setUseAlternativeNames(@Nullable Boolean useAlternativeNames) {
		this.useAlternativeNames = useAlternativeNames;
	}

	public @Nullable Boolean getAllowTrailingComma() {
		return this.allowTrailingComma;
	}

	public void setAllowTrailingComma(@Nullable Boolean allowTrailingComma) {
		this.allowTrailingComma = allowTrailingComma;
	}

	public @Nullable Boolean getAllowComments() {
		return this.allowComments;
	}

	public void setAllowComments(@Nullable Boolean allowComments) {
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
