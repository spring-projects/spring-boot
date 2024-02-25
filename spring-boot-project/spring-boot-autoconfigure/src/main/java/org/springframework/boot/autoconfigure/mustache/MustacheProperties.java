/*
 * Copyright 2012-2022 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for Mustache.
 *
 * @author Dave Syer
 * @since 1.2.2
 */
@ConfigurationProperties(prefix = "spring.mustache")
public class MustacheProperties {

	private static final MimeType DEFAULT_CONTENT_TYPE = MimeType.valueOf("text/html");

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public static final String DEFAULT_PREFIX = "classpath:/templates/";

	public static final String DEFAULT_SUFFIX = ".mustache";

	private final Servlet servlet = new Servlet();

	private final Reactive reactive = new Reactive();

	/**
	 * View names that can be resolved.
	 */
	private String[] viewNames;

	/**
	 * Name of the RequestContext attribute for all views.
	 */
	private String requestContextAttribute;

	/**
	 * Whether to enable MVC view resolution for Mustache.
	 */
	private boolean enabled = true;

	/**
	 * Template encoding.
	 */
	private Charset charset = DEFAULT_CHARSET;

	/**
	 * Whether to check that the templates location exists.
	 */
	private boolean checkTemplateLocation = true;

	/**
	 * Prefix to apply to template names.
	 */
	private String prefix = DEFAULT_PREFIX;

	/**
	 * Suffix to apply to template names.
	 */
	private String suffix = DEFAULT_SUFFIX;

	/**
     * Returns the servlet associated with this MustacheProperties object.
     *
     * @return the servlet associated with this MustacheProperties object
     */
    public Servlet getServlet() {
		return this.servlet;
	}

	/**
     * Returns the Reactive object associated with this MustacheProperties instance.
     *
     * @return the Reactive object
     */
    public Reactive getReactive() {
		return this.reactive;
	}

	/**
     * Returns the prefix used for variable placeholders in Mustache templates.
     *
     * @return the prefix used for variable placeholders
     */
    public String getPrefix() {
		return this.prefix;
	}

	/**
     * Sets the prefix for Mustache template files.
     * 
     * @param prefix the prefix to be set
     */
    public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
     * Returns the suffix used for resolving view templates.
     * 
     * @return the suffix used for resolving view templates
     */
    public String getSuffix() {
		return this.suffix;
	}

	/**
     * Sets the suffix for the Mustache template files.
     * 
     * @param suffix the suffix to be set
     */
    public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
     * Returns an array of view names.
     *
     * @return an array of view names
     */
    public String[] getViewNames() {
		return this.viewNames;
	}

	/**
     * Sets the array of view names.
     * 
     * @param viewNames the array of view names to be set
     */
    public void setViewNames(String[] viewNames) {
		this.viewNames = viewNames;
	}

	/**
     * Returns the value of the request context attribute.
     *
     * @return the value of the request context attribute
     */
    public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
     * Sets the value of the request context attribute.
     * 
     * @param requestContextAttribute the value to set for the request context attribute
     */
    public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
     * Returns the charset used by the Mustache template engine.
     * 
     * @return the charset used by the Mustache template engine
     */
    public Charset getCharset() {
		return this.charset;
	}

	/**
     * Returns the name of the character set used by this MustacheProperties object.
     * 
     * @return the name of the character set, or null if no character set is set
     */
    public String getCharsetName() {
		return (this.charset != null) ? this.charset.name() : null;
	}

	/**
     * Sets the charset for the MustacheProperties.
     * 
     * @param charset the charset to be set
     */
    public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
     * Returns the value indicating whether to check the template location.
     * 
     * @return {@code true} if the template location should be checked, {@code false} otherwise.
     */
    public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	/**
     * Sets the flag to check the template location.
     * 
     * @param checkTemplateLocation the flag indicating whether to check the template location
     */
    public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
	}

	/**
     * Returns the current status of the enabled flag.
     *
     * @return {@code true} if the enabled flag is set, {@code false} otherwise.
     */
    public boolean isEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the MustacheProperties.
     * 
     * @param enabled the enabled status to be set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Servlet class.
     */
    public static class Servlet {

		/**
		 * Whether HttpServletRequest attributes are allowed to override (hide) controller
		 * generated model attributes of the same name.
		 */
		private boolean allowRequestOverride = false;

		/**
		 * Whether HttpSession attributes are allowed to override (hide) controller
		 * generated model attributes of the same name.
		 */
		private boolean allowSessionOverride = false;

		/**
		 * Whether to enable template caching.
		 */
		private boolean cache;

		/**
		 * Content-Type value.
		 */
		private MimeType contentType = DEFAULT_CONTENT_TYPE;

		/**
		 * Whether all request attributes should be added to the model prior to merging
		 * with the template.
		 */
		private boolean exposeRequestAttributes = false;

		/**
		 * Whether all HttpSession attributes should be added to the model prior to
		 * merging with the template.
		 */
		private boolean exposeSessionAttributes = false;

		/**
		 * Whether to expose a RequestContext for use by Spring's macro library, under the
		 * name "springMacroRequestContext".
		 */
		private boolean exposeSpringMacroHelpers = true;

		/**
         * Returns a boolean value indicating whether the request override is allowed.
         *
         * @return {@code true} if the request override is allowed, {@code false} otherwise.
         */
        public boolean isAllowRequestOverride() {
			return this.allowRequestOverride;
		}

		/**
         * Sets whether the servlet allows request override.
         * 
         * @param allowRequestOverride true if the servlet allows request override, false otherwise
         */
        public void setAllowRequestOverride(boolean allowRequestOverride) {
			this.allowRequestOverride = allowRequestOverride;
		}

		/**
         * Returns a boolean value indicating whether session override is allowed.
         * 
         * @return true if session override is allowed, false otherwise
         */
        public boolean isAllowSessionOverride() {
			return this.allowSessionOverride;
		}

		/**
         * Sets whether the session can be overridden.
         * 
         * @param allowSessionOverride true if the session can be overridden, false otherwise
         */
        public void setAllowSessionOverride(boolean allowSessionOverride) {
			this.allowSessionOverride = allowSessionOverride;
		}

		/**
         * Returns a boolean value indicating whether the cache is enabled or not.
         * 
         * @return true if the cache is enabled, false otherwise
         */
        public boolean isCache() {
			return this.cache;
		}

		/**
         * Sets the cache flag for the servlet.
         * 
         * @param cache the cache flag to be set
         */
        public void setCache(boolean cache) {
			this.cache = cache;
		}

		/**
         * Returns the content type of the response.
         * 
         * @return the content type of the response
         */
        public MimeType getContentType() {
			return this.contentType;
		}

		/**
         * Sets the content type of the response.
         * 
         * @param contentType the MIME type of the content
         */
        public void setContentType(MimeType contentType) {
			this.contentType = contentType;
		}

		/**
         * Returns a boolean value indicating whether the request attributes are exposed.
         * 
         * @return true if the request attributes are exposed, false otherwise
         */
        public boolean isExposeRequestAttributes() {
			return this.exposeRequestAttributes;
		}

		/**
         * Sets whether to expose request attributes.
         * 
         * @param exposeRequestAttributes true to expose request attributes, false otherwise
         */
        public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
			this.exposeRequestAttributes = exposeRequestAttributes;
		}

		/**
         * Returns a boolean value indicating whether the session attributes are exposed.
         * 
         * @return true if the session attributes are exposed, false otherwise
         */
        public boolean isExposeSessionAttributes() {
			return this.exposeSessionAttributes;
		}

		/**
         * Sets whether to expose session attributes.
         * 
         * @param exposeSessionAttributes true to expose session attributes, false otherwise
         */
        public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
			this.exposeSessionAttributes = exposeSessionAttributes;
		}

		/**
         * Returns a boolean value indicating whether the Spring macro helpers are exposed.
         * 
         * @return true if the Spring macro helpers are exposed, false otherwise
         */
        public boolean isExposeSpringMacroHelpers() {
			return this.exposeSpringMacroHelpers;
		}

		/**
         * Sets the flag indicating whether to expose Spring macro helpers.
         * 
         * @param exposeSpringMacroHelpers true to expose Spring macro helpers, false otherwise
         */
        public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
			this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
		}

	}

	/**
     * Reactive class.
     */
    public static class Reactive {

		/**
		 * Media types supported by Mustache views.
		 */
		private List<MediaType> mediaTypes;

		/**
         * Returns the list of media types supported by this Reactive instance.
         *
         * @return the list of media types
         */
        public List<MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		/**
         * Sets the list of media types for the Reactive class.
         * 
         * @param mediaTypes the list of media types to be set
         */
        public void setMediaTypes(List<MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

	}

}
