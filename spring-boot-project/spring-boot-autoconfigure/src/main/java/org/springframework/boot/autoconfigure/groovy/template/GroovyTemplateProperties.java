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

package org.springframework.boot.autoconfigure.groovy.template;

import org.springframework.boot.autoconfigure.template.AbstractTemplateViewResolverProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Groovy templates.
 *
 * @author Dave Syer
 * @author Marten Deinum
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.groovy.template", ignoreUnknownFields = true)
public class GroovyTemplateProperties extends AbstractTemplateViewResolverProperties {

	public static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:/templates/";

	public static final String DEFAULT_PREFIX = "";

	public static final String DEFAULT_SUFFIX = ".tpl";

	public static final String DEFAULT_REQUEST_CONTEXT_ATTRIBUTE = "spring";

	/**
	 * Template path.
	 */
	private String resourceLoaderPath = DEFAULT_RESOURCE_LOADER_PATH;

	public GroovyTemplateProperties() {
		super(DEFAULT_PREFIX, DEFAULT_SUFFIX);
		setRequestContextAttribute(DEFAULT_REQUEST_CONTEXT_ATTRIBUTE);
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

}
