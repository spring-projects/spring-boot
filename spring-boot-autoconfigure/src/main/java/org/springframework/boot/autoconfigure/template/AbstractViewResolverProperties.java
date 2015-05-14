/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.template;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.servlet.ViewResolver;

/**
 * Base class for {@link ConfigurationProperties} of a {@link ViewResolver}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.2.0
 * @see AbstractTemplateViewResolverProperties
 */
public abstract class AbstractViewResolverProperties {

	/**
	 * Enable MVC view resolution for this technology.
	 */
	private boolean enabled = true;

	/**
	 * Enable template caching.
	 */
	private boolean cache;

	/**
	 * Content-Type value.
	 */
	private String contentType = "text/html";

	/**
	 * Template encoding.
	 */
	private String charset = "UTF-8";

	/**
	 * White list of view names that can be resolved.
	 */
	private String[] viewNames;

	/**
	 * Check that the templates location exists.
	 */
	private boolean checkTemplateLocation = true;

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

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
						+ this.charset);
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

}
