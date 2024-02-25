/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.template;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.ViewResolver;

/**
 * Base class for {@link ConfigurationProperties @ConfigurationProperties} of a
 * {@link ViewResolver}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.2.0
 * @see AbstractTemplateViewResolverProperties
 */
public abstract class AbstractViewResolverProperties {

	private static final MimeType DEFAULT_CONTENT_TYPE = MimeType.valueOf("text/html");

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Whether to enable MVC view resolution for this technology.
	 */
	private boolean enabled = true;

	/**
	 * Whether to enable template caching.
	 */
	private boolean cache;

	/**
	 * Content-Type value.
	 */
	private MimeType contentType = DEFAULT_CONTENT_TYPE;

	/**
	 * Template encoding.
	 */
	private Charset charset = DEFAULT_CHARSET;

	/**
	 * View names that can be resolved.
	 */
	private String[] viewNames;

	/**
	 * Whether to check that the templates location exists.
	 */
	private boolean checkTemplateLocation = true;

	/**
     * Sets the enabled status of the view resolver.
     * 
     * @param enabled the enabled status to set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns the current status of the enabled property.
     *
     * @return {@code true} if the view resolver is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the flag indicating whether to check the template location.
     * 
     * @param checkTemplateLocation the flag indicating whether to check the template location
     */
    public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
	}

	/**
     * Returns the value indicating whether to check the template location.
     * 
     * @return {@code true} if the template location should be checked, {@code false} otherwise
     */
    public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	/**
     * Returns an array of view names.
     *
     * @return the array of view names
     */
    public String[] getViewNames() {
		return this.viewNames;
	}

	/**
     * Sets the array of view names for this AbstractViewResolverProperties object.
     * 
     * @param viewNames the array of view names to be set
     */
    public void setViewNames(String[] viewNames) {
		this.viewNames = viewNames;
	}

	/**
     * Returns a boolean value indicating whether caching is enabled for this view resolver.
     *
     * @return {@code true} if caching is enabled, {@code false} otherwise
     */
    public boolean isCache() {
		return this.cache;
	}

	/**
     * Sets the value indicating whether caching is enabled for the view resolver.
     * 
     * @param cache the value indicating whether caching is enabled
     */
    public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
     * Returns the content type of the view resolver.
     * 
     * @return The content type as a MimeType object.
     */
    public MimeType getContentType() {
		if (this.contentType.getCharset() == null) {
			Map<String, String> parameters = new LinkedHashMap<>();
			parameters.put("charset", this.charset.name());
			parameters.putAll(this.contentType.getParameters());
			return new MimeType(this.contentType, parameters);
		}
		return this.contentType;
	}

	/**
     * Sets the content type for the view resolver.
     * 
     * @param contentType the MIME type to be set as the content type
     */
    public void setContentType(MimeType contentType) {
		this.contentType = contentType;
	}

	/**
     * Returns the charset used by this view resolver.
     *
     * @return the charset used by this view resolver
     */
    public Charset getCharset() {
		return this.charset;
	}

	/**
     * Returns the name of the character set used by this AbstractViewResolverProperties object.
     * 
     * @return the name of the character set, or null if no character set is set
     */
    public String getCharsetName() {
		return (this.charset != null) ? this.charset.name() : null;
	}

	/**
     * Sets the character encoding to be used for rendering views.
     * 
     * @param charset the character encoding to be set
     */
    public void setCharset(Charset charset) {
		this.charset = charset;
	}

}
