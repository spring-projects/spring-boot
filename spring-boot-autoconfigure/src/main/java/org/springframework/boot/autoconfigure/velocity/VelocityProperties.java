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

package org.springframework.boot.autoconfigure.velocity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.template.AbstractTemplateViewResolverProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

/**
 * {@link ConfigurationProperties} for configuring Velocity
 * 
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.velocity")
public class VelocityProperties extends AbstractTemplateViewResolverProperties {

	public static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:/templates/";

	public static final String DEFAULT_PREFIX = "";

	public static final String DEFAULT_SUFFIX = ".vm";

	private String dateToolAttribute;

	private String numberToolAttribute;

	private Map<String, String> properties = new HashMap<String, String>();

	private String resourceLoaderPath = DEFAULT_RESOURCE_LOADER_PATH;

	private String toolboxConfigLocation;

	public VelocityProperties() {
		super(DEFAULT_PREFIX, DEFAULT_SUFFIX);
	}

	public String getDateToolAttribute() {
		return this.dateToolAttribute;
	}

	public void setDateToolAttribute(String dateToolAttribute) {
		this.dateToolAttribute = dateToolAttribute;
	}

	public String getNumberToolAttribute() {
		return this.numberToolAttribute;
	}

	public void setNumberToolAttribute(String numberToolAttribute) {
		this.numberToolAttribute = numberToolAttribute;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public String getToolboxConfigLocation() {
		return this.toolboxConfigLocation;
	}

	public void setToolboxConfigLocation(String toolboxConfigLocation) {
		this.toolboxConfigLocation = toolboxConfigLocation;
	}

	/**
	 * Apply the given properties to a {@link VelocityViewResolver}.
	 * @param resolver the resolver to apply the properties to.
	 */
	public void applyToViewResolver(VelocityViewResolver resolver) {
		super.applyToViewResolver(resolver);
		resolver.setToolboxConfigLocation(getToolboxConfigLocation());
		resolver.setDateToolAttribute(getDateToolAttribute());
		resolver.setNumberToolAttribute(getNumberToolAttribute());
	}

}
