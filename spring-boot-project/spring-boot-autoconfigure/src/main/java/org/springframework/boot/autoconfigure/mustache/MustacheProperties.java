/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import org.springframework.boot.autoconfigure.template.AbstractTemplateViewResolverProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for Mustache.
 *
 * @author Dave Syer
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

}
