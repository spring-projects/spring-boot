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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.nio.charset.Charset;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MimeType;

/**
 * Properties for Thymeleaf.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.thymeleaf")
public class ThymeleafProperties {

	private static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

	private static final MimeType DEFAULT_CONTENT_TYPE = MimeType.valueOf("text/html");

	public static final String DEFAULT_PREFIX = "classpath:/templates/";

	public static final String DEFAULT_SUFFIX = ".html";

	/**
	 * Check that the template exists before rendering it (Thymeleaf 3+).
	 */
	private boolean checkTemplate = true;

	/**
	 * Check that the templates location exists.
	 */
	private boolean checkTemplateLocation = true;

	/**
	 * Prefix that gets prepended to view names when building a URL.
	 */
	private String prefix = DEFAULT_PREFIX;

	/**
	 * Suffix that gets appended to view names when building a URL.
	 */
	private String suffix = DEFAULT_SUFFIX;

	/**
	 * Template mode to be applied to templates. See also StandardTemplateModeHandlers.
	 */
	private String mode = "HTML5";

	/**
	 * Template encoding.
	 */
	private Charset encoding = DEFAULT_ENCODING;

	/**
	 * Content-Type value.
	 */
	private MimeType contentType = DEFAULT_CONTENT_TYPE;

	/**
	 * Enable template caching.
	 */
	private boolean cache = true;

	/**
	 * Order of the template resolver in the chain. By default, the template resolver is
	 * first in the chain. Order start at 1 and should only be set if you have defined
	 * additional "TemplateResolver" beans.
	 */
	private Integer templateResolverOrder;

	/**
	 * Comma-separated list of view names that can be resolved.
	 */
	private String[] viewNames;

	/**
	 * Comma-separated list of view names that should be excluded from resolution.
	 */
	private String[] excludedViewNames;

	/**
	 * Enable MVC Thymeleaf view resolution.
	 */
	private boolean enabled = true;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isCheckTemplate() {
		return this.checkTemplate;
	}

	public void setCheckTemplate(boolean checkTemplate) {
		this.checkTemplate = checkTemplate;
	}

	public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
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

	public String getMode() {
		return this.mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public Charset getEncoding() {
		return this.encoding;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	public MimeType getContentType() {
		return this.contentType;
	}

	public void setContentType(MimeType contentType) {
		this.contentType = contentType;
	}

	public boolean isCache() {
		return this.cache;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	public Integer getTemplateResolverOrder() {
		return this.templateResolverOrder;
	}

	public void setTemplateResolverOrder(Integer templateResolverOrder) {
		this.templateResolverOrder = templateResolverOrder;
	}

	public String[] getExcludedViewNames() {
		return this.excludedViewNames;
	}

	public void setExcludedViewNames(String[] excludedViewNames) {
		this.excludedViewNames = excludedViewNames;
	}

	public String[] getViewNames() {
		return this.viewNames;
	}

	public void setViewNames(String[] viewNames) {
		this.viewNames = viewNames;
	}

}
