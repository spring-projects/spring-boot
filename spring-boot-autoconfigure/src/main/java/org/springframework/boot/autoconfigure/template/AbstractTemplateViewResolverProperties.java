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

package org.springframework.boot.autoconfigure.template;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * Abstract base class for {@link ConfigurationProperties} for
 * {@link AbstractTemplateViewResolver view resolvers}.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public abstract class AbstractTemplateViewResolverProperties {

	private String prefix;

	private String suffix;

	private boolean cache;

	private String contentType = "text/html";

	private String charSet = "UTF-8";

	private String[] viewNames;

	private boolean checkTemplateLocation = true;

	private String requestContextAttribute;

	private boolean exposeRequestAttributes = false;

	private boolean exposeSessionAttributes = false;

	private boolean allowRequestOverride = false;

	private boolean exposeSpringMacroHelpers = true;

	protected AbstractTemplateViewResolverProperties(String defaultPrefix,
			String defaultSuffix) {
		this.prefix = defaultPrefix;
		this.suffix = defaultSuffix;
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

	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	public boolean isExposeRequestAttributes() {
		return this.exposeRequestAttributes;
	}

	public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
		this.exposeRequestAttributes = exposeRequestAttributes;
	}

	public boolean isExposeSessionAttributes() {
		return this.exposeSessionAttributes;
	}

	public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
		this.exposeSessionAttributes = exposeSessionAttributes;
	}

	public boolean isAllowRequestOverride() {
		return this.allowRequestOverride;
	}

	public void setAllowRequestOverride(boolean allowRequestOverride) {
		this.allowRequestOverride = allowRequestOverride;
	}

	public boolean isExposeSpringMacroHelpers() {
		return this.exposeSpringMacroHelpers;
	}

	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}
}
