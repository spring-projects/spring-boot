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

	public Servlet getServlet() {
		return this.servlet;
	}

	public Reactive getReactive() {
		return this.reactive;
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

	public String[] getViewNames() {
		return this.viewNames;
	}

	public void setViewNames(String[] viewNames) {
		this.viewNames = viewNames;
	}

	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	public void setRequestContextAttribute(String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	public Charset getCharset() {
		return this.charset;
	}

	public String getCharsetName() {
		return (this.charset != null) ? this.charset.name() : null;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

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

		public boolean isAllowRequestOverride() {
			return this.allowRequestOverride;
		}

		public void setAllowRequestOverride(boolean allowRequestOverride) {
			this.allowRequestOverride = allowRequestOverride;
		}

		public boolean isAllowSessionOverride() {
			return this.allowSessionOverride;
		}

		public void setAllowSessionOverride(boolean allowSessionOverride) {
			this.allowSessionOverride = allowSessionOverride;
		}

		public boolean isCache() {
			return this.cache;
		}

		public void setCache(boolean cache) {
			this.cache = cache;
		}

		public MimeType getContentType() {
			return this.contentType;
		}

		public void setContentType(MimeType contentType) {
			this.contentType = contentType;
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

		public boolean isExposeSpringMacroHelpers() {
			return this.exposeSpringMacroHelpers;
		}

		public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
			this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
		}

	}

	public static class Reactive {

		/**
		 * Media types supported by Mustache views.
		 */
		private List<MediaType> mediaTypes;

		public List<MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		public void setMediaTypes(List<MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

	}

}
