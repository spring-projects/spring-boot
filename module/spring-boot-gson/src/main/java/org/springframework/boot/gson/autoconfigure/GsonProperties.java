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

package org.springframework.boot.gson.autoconfigure;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.LongSerializationPolicy;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure {@link Gson}.
 *
 * @author Ivan Golovko
 * @since 4.0.0
 */
@ConfigurationProperties("spring.gson")
public class GsonProperties {

	/**
	 * Whether to generate non-executable JSON by prefixing the output with some special
	 * text.
	 */
	private @Nullable Boolean generateNonExecutableJson;

	/**
	 * Whether to exclude all fields from consideration for serialization or
	 * deserialization that do not have the "Expose" annotation.
	 */
	private @Nullable Boolean excludeFieldsWithoutExposeAnnotation;

	/**
	 * Whether to serialize null fields.
	 */
	private @Nullable Boolean serializeNulls;

	/**
	 * Whether to enable serialization of complex map keys (i.e. non-primitives).
	 */
	private @Nullable Boolean enableComplexMapKeySerialization;

	/**
	 * Whether to exclude inner classes during serialization.
	 */
	private @Nullable Boolean disableInnerClassSerialization;

	/**
	 * Serialization policy for Long and long types.
	 */
	private @Nullable LongSerializationPolicy longSerializationPolicy;

	/**
	 * Naming policy that should be applied to an object's field during serialization and
	 * deserialization.
	 */
	private @Nullable FieldNamingPolicy fieldNamingPolicy;

	/**
	 * Whether to output serialized JSON that fits in a page for pretty printing.
	 */
	private @Nullable Boolean prettyPrinting;

	/**
	 * Sets how strictly the RFC 8259 specification will be enforced when reading and
	 * writing JSON.
	 */
	private @Nullable Strictness strictness;

	/**
	 * Whether to disable the escaping of HTML characters such as '<', '>', etc.
	 */
	private @Nullable Boolean disableHtmlEscaping;

	/**
	 * Format to use when serializing Date objects.
	 */
	private @Nullable String dateFormat;

	public @Nullable Boolean getGenerateNonExecutableJson() {
		return this.generateNonExecutableJson;
	}

	public void setGenerateNonExecutableJson(@Nullable Boolean generateNonExecutableJson) {
		this.generateNonExecutableJson = generateNonExecutableJson;
	}

	public @Nullable Boolean getExcludeFieldsWithoutExposeAnnotation() {
		return this.excludeFieldsWithoutExposeAnnotation;
	}

	public void setExcludeFieldsWithoutExposeAnnotation(@Nullable Boolean excludeFieldsWithoutExposeAnnotation) {
		this.excludeFieldsWithoutExposeAnnotation = excludeFieldsWithoutExposeAnnotation;
	}

	public @Nullable Boolean getSerializeNulls() {
		return this.serializeNulls;
	}

	public void setSerializeNulls(@Nullable Boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	public @Nullable Boolean getEnableComplexMapKeySerialization() {
		return this.enableComplexMapKeySerialization;
	}

	public void setEnableComplexMapKeySerialization(@Nullable Boolean enableComplexMapKeySerialization) {
		this.enableComplexMapKeySerialization = enableComplexMapKeySerialization;
	}

	public @Nullable Boolean getDisableInnerClassSerialization() {
		return this.disableInnerClassSerialization;
	}

	public void setDisableInnerClassSerialization(@Nullable Boolean disableInnerClassSerialization) {
		this.disableInnerClassSerialization = disableInnerClassSerialization;
	}

	public @Nullable LongSerializationPolicy getLongSerializationPolicy() {
		return this.longSerializationPolicy;
	}

	public void setLongSerializationPolicy(@Nullable LongSerializationPolicy longSerializationPolicy) {
		this.longSerializationPolicy = longSerializationPolicy;
	}

	public @Nullable FieldNamingPolicy getFieldNamingPolicy() {
		return this.fieldNamingPolicy;
	}

	public void setFieldNamingPolicy(@Nullable FieldNamingPolicy fieldNamingPolicy) {
		this.fieldNamingPolicy = fieldNamingPolicy;
	}

	public @Nullable Boolean getPrettyPrinting() {
		return this.prettyPrinting;
	}

	public void setPrettyPrinting(@Nullable Boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	public @Nullable Strictness getStrictness() {
		return this.strictness;
	}

	public void setStrictness(@Nullable Strictness strictness) {
		this.strictness = strictness;
	}

	public void setLenient(@Nullable Boolean lenient) {
		setStrictness((lenient != null && lenient) ? Strictness.LENIENT : Strictness.STRICT);
	}

	public @Nullable Boolean getDisableHtmlEscaping() {
		return this.disableHtmlEscaping;
	}

	public void setDisableHtmlEscaping(@Nullable Boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	public @Nullable String getDateFormat() {
		return this.dateFormat;
	}

	public void setDateFormat(@Nullable String dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Enumeration of levels of strictness. Values are the same as those on
	 * {@link com.google.gson.Strictness} that was introduced in Gson 2.11. To maximize
	 * backwards compatibility, the Gson enum is not used directly.
	 */
	public enum Strictness {

		/**
		 * Lenient compliance.
		 */
		LENIENT,

		/**
		 * Strict compliance with some small deviations for legacy reasons.
		 */
		LEGACY_STRICT,

		/**
		 * Strict compliance.
		 */
		STRICT

	}

}
