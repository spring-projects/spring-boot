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

package org.springframework.boot.kotlinx.serialization.json.autoconfigure;

import kotlinx.serialization.json.ClassDiscriminatorMode;
import kotlinx.serialization.json.Json;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure Kotlinx Serialization {@link Json}.
 *
 * @author Dmitry Sulman
 * @since 4.0.0
 */
@ConfigurationProperties("spring.kotlinx.serialization.json")
public class KotlinxSerializationJsonProperties {

	/**
	 * Specifies JsonNamingStrategy that should be used for all properties in classes for
	 * serialization and deserialization.
	 */
	private @Nullable JsonNamingStrategy namingStrategy;

	/**
	 * Whether resulting JSON should be pretty-printed.
	 */
	private boolean prettyPrint;

	/**
	 * Whether parser should operate in lenient mode, removing the JSON specification
	 * restriction (RFC-4627) and being more liberal to malformed input.
	 */
	private boolean lenient;

	/**
	 * Whether encounters of unknown properties in the input JSON should be ignored
	 * instead of throwing SerializationException.
	 */
	private boolean ignoreUnknownKeys;

	/**
	 * Whether default values of Kotlin properties should be encoded.
	 */
	private boolean encodeDefaults;

	/**
	 * Whether null values should be encoded for nullable properties and must be present
	 * in JSON object during decoding.
	 */
	private boolean explicitNulls = true;

	/**
	 * Whether to coerce incorrect JSON values.
	 */
	private boolean coerceInputValues;

	/**
	 * Whether to allow structured objects to be serialized as map keys by changing the
	 * serialized form of the map from JSON object (key-value pairs) to flat array like
	 * [k1, v1, k2, v2].
	 */
	private boolean allowStructuredMapKeys;

	/**
	 * Whether to remove the JSON specification restriction on special floating-point
	 * values such as 'NaN' and 'Infinity' and allow their serialization and
	 * deserialization as float literals without quotes.
	 */
	private boolean allowSpecialFloatingPointValues;

	/**
	 * Name of the class descriptor property for polymorphic serialization.
	 */
	private String classDiscriminator = "type";

	/**
	 * Defines which classes and objects should have class discriminator added to the
	 * output.
	 */
	private ClassDiscriminatorMode classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC;

	/**
	 * Whether enum values are decoded in a case-insensitive manner.
	 */
	private boolean decodeEnumsCaseInsensitive;

	/**
	 * Whether Json instance makes use of JsonNames annotation.
	 */
	private boolean useAlternativeNames = true;

	/**
	 * Whether to allow parser to accept trailing commas in JSON objects and arrays.
	 */
	private boolean allowTrailingComma;

	/**
	 * Whether to allow parser to accept C/Java-style comments in JSON input.
	 */
	private boolean allowComments;

	public @Nullable JsonNamingStrategy getNamingStrategy() {
		return this.namingStrategy;
	}

	public void setNamingStrategy(@Nullable JsonNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public boolean isPrettyPrint() {
		return this.prettyPrint;
	}

	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	public boolean isLenient() {
		return this.lenient;
	}

	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	public boolean isIgnoreUnknownKeys() {
		return this.ignoreUnknownKeys;
	}

	public void setIgnoreUnknownKeys(boolean ignoreUnknownKeys) {
		this.ignoreUnknownKeys = ignoreUnknownKeys;
	}

	public boolean isEncodeDefaults() {
		return this.encodeDefaults;
	}

	public void setEncodeDefaults(boolean encodeDefaults) {
		this.encodeDefaults = encodeDefaults;
	}

	public boolean isExplicitNulls() {
		return this.explicitNulls;
	}

	public void setExplicitNulls(boolean explicitNulls) {
		this.explicitNulls = explicitNulls;
	}

	public boolean isCoerceInputValues() {
		return this.coerceInputValues;
	}

	public void setCoerceInputValues(boolean coerceInputValues) {
		this.coerceInputValues = coerceInputValues;
	}

	public boolean isAllowStructuredMapKeys() {
		return this.allowStructuredMapKeys;
	}

	public void setAllowStructuredMapKeys(boolean allowStructuredMapKeys) {
		this.allowStructuredMapKeys = allowStructuredMapKeys;
	}

	public boolean isAllowSpecialFloatingPointValues() {
		return this.allowSpecialFloatingPointValues;
	}

	public void setAllowSpecialFloatingPointValues(boolean allowSpecialFloatingPointValues) {
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

	public boolean isDecodeEnumsCaseInsensitive() {
		return this.decodeEnumsCaseInsensitive;
	}

	public void setDecodeEnumsCaseInsensitive(boolean decodeEnumsCaseInsensitive) {
		this.decodeEnumsCaseInsensitive = decodeEnumsCaseInsensitive;
	}

	public boolean isUseAlternativeNames() {
		return this.useAlternativeNames;
	}

	public void setUseAlternativeNames(boolean useAlternativeNames) {
		this.useAlternativeNames = useAlternativeNames;
	}

	public boolean isAllowTrailingComma() {
		return this.allowTrailingComma;
	}

	public void setAllowTrailingComma(boolean allowTrailingComma) {
		this.allowTrailingComma = allowTrailingComma;
	}

	public boolean isAllowComments() {
		return this.allowComments;
	}

	public void setAllowComments(boolean allowComments) {
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
