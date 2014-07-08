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

package org.springframework.boot.autoconfigure.groovy.template;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Groovy templates.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.groovy.template", ignoreUnknownFields = false)
public class GroovyTemplateProperties {

	public static final String DEFAULT_PREFIX = "classpath:/templates/";

	public static final String DEFAULT_SUFFIX = ".tpl";

	private String prefix = DEFAULT_PREFIX;

	private String suffix = DEFAULT_SUFFIX;

	private boolean cache;

	private String contentType = "text/html";

	private String charSet = "UTF-8";

	private String[] viewNames;

	private boolean checkTemplateLocation = false;

	private Map<String, Object> configuration = new HashMap<String, Object>();

	public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
	}

	public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	public String[] getViewNames() {
		return this.viewNames;
	}

	public void setViewNames(String[] viewNames) {
		this.viewNames = viewNames;
	}

	public boolean isCache() {
		return this.cache;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	public String getContentType() {
		return this.contentType
				+ (this.contentType.contains(";charset=") ? "" : ";charset="
						+ this.charSet);
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getCharSet() {
		return this.charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return this.suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	public Map<String, Object> getConfiguration() {
		return this.configuration;
	}

}
