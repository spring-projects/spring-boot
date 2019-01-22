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

package org.springframework.boot.autoconfigure.mustache;

import org.springframework.boot.autoconfigure.template.AbstractTemplateViewResolverProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for Mustache.
 *
 * @author Dave Syer
 * @author Artsiom Yudovin
 * @since 1.2.2
 */
@ConfigurationProperties(prefix = "spring.mustache")
public class MustacheProperties extends AbstractTemplateViewResolverProperties {

	public static final String DEFAULT_PREFIX = "classpath:/templates/";

	public static final String DEFAULT_SUFFIX = ".mustache";

	/**
	 * Prefix to apply to template names.
	 */
	private String prefix = DEFAULT_PREFIX;

	/**
	 * Suffix to apply to template names.
	 */
	private String suffix = DEFAULT_SUFFIX;

	/**
	 * Default value for any variable that is missing.
	 */
	private String defaultValue;

	/**
	 * Value for any variable that resolves to null.
	 */
	private String nullValue;

	/**
	 * Handles converting objects to strings.
	 */
	private final Formatter formatter = new Formatter();

	/**
	 * Handles escaping characters in substituted text.
	 */
	private final Escaper escaper = new Escaper();

	public MustacheProperties() {
		super(DEFAULT_PREFIX, DEFAULT_SUFFIX);
	}

	@Override
	public String getPrefix() {
		return this.prefix;
	}

	@Override
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public String getSuffix() {
		return this.suffix;
	}

	@Override
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getNullValue() {
		return this.nullValue;
	}

	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public Formatter getFormatter() {
		return this.formatter;
	}

	public Escaper getEscaper() {
		return this.escaper;
	}

	public static class Formatter {

		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public static class Escaper {

		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

}
