/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.freemarker.autoconfigure;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring FreeMarker.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.freemarker")
public class FreeMarkerProperties {

	public static final String DEFAULT_TEMPLATE_LOADER_PATH = "classpath:/templates/";

	public static final String DEFAULT_PREFIX = "";

	public static final String DEFAULT_SUFFIX = ".ftlh";

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
	 * Prefix that gets prepended to view names when building a URL.
	 */
	private String prefix = DEFAULT_PREFIX;

	/**
	 * Suffix that gets appended to view names when building a URL.
	 */
	private String suffix = DEFAULT_SUFFIX;

	/**
	 * Name of the RequestContext attribute for all views.
	 */
	private String requestContextAttribute;

	/**
	 * Whether all request attributes should be added to the model prior to merging with
	 * the template.
	 */
	private boolean exposeRequestAttributes = false;

	/**
	 * Whether all HttpSession attributes should be added to the model prior to merging
	 * with the template.
	 */
	private boolean exposeSessionAttributes = false;

	/**
	 * Whether HttpServletRequest attributes are allowed to override (hide) controller
	 * generated model attributes of the same name.
	 */
	private boolean allowRequestOverride = false;

	/**
	 * Whether to expose a RequestContext for use by Spring's macro library, under the
	 * name "springMacroRequestContext".
	 */
	private boolean exposeSpringMacroHelpers = true;

	/**
	 * Whether HttpSession attributes are allowed to override (hide) controller generated
	 * model attributes of the same name.
	 */
	private boolean allowSessionOverride = false;

	/**
	 * Well-known FreeMarker keys which are passed to FreeMarker's Configuration.
	 */
	private Map<String, String> settings = new HashMap<>();

	/**
	 * List of template paths.
	 */
	private String[] templateLoaderPath = new String[] { DEFAULT_TEMPLATE_LOADER_PATH };

	/**
	 * Whether to prefer file system access for template loading to enable hot detection
	 * of template changes. When a template path is detected as a directory, templates are
	 * loaded from the directory only and other matching classpath locations will not be
	 * considered.
	 */
	private boolean preferFileSystemAccess;

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

	public MimeType getContentType() {
		if (this.contentType.getCharset() == null) {
			Map<String, String> parameters = new LinkedHashMap<>();
			parameters.put("charset", this.charset.name());
			parameters.putAll(this.contentType.getParameters());
			return new MimeType(this.contentType, parameters);
		}
		return this.contentType;
	}

	public void setContentType(MimeType contentType) {
		this.contentType = contentType;
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

	public Map<String, String> getSettings() {
		return this.settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public String[] getTemplateLoaderPath() {
		return this.templateLoaderPath;
	}

	public void setTemplateLoaderPath(String... templateLoaderPaths) {
		this.templateLoaderPath = templateLoaderPaths;
	}

	public boolean isPreferFileSystemAccess() {
		return this.preferFileSystemAccess;
	}

	public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
		this.preferFileSystemAccess = preferFileSystemAccess;
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

	public boolean isAllowSessionOverride() {
		return this.allowSessionOverride;
	}

	public void setAllowSessionOverride(boolean allowSessionOverride) {
		this.allowSessionOverride = allowSessionOverride;
	}

	public boolean isExposeSpringMacroHelpers() {
		return this.exposeSpringMacroHelpers;
	}

	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}

	/**
	 * Apply the given properties to a {@link AbstractTemplateViewResolver}. Use Object in
	 * signature to avoid runtime dependency on MVC, which means that the template engine
	 * can be used in a non-web application.
	 * @param viewResolver the resolver to apply the properties to.
	 */
	public void applyToMvcViewResolver(Object viewResolver) {
		Assert.isInstanceOf(AbstractTemplateViewResolver.class, viewResolver,
				() -> "ViewResolver is not an instance of AbstractTemplateViewResolver :" + viewResolver);
		AbstractTemplateViewResolver resolver = (AbstractTemplateViewResolver) viewResolver;
		resolver.setPrefix(getPrefix());
		resolver.setSuffix(getSuffix());
		resolver.setCache(isCache());
		if (getContentType() != null) {
			resolver.setContentType(getContentType().toString());
		}
		resolver.setViewNames(getViewNames());
		resolver.setExposeRequestAttributes(isExposeRequestAttributes());
		resolver.setAllowRequestOverride(isAllowRequestOverride());
		resolver.setAllowSessionOverride(isAllowSessionOverride());
		resolver.setExposeSessionAttributes(isExposeSessionAttributes());
		resolver.setExposeSpringMacroHelpers(isExposeSpringMacroHelpers());
		resolver.setRequestContextAttribute(getRequestContextAttribute());
		// The resolver usually acts as a fallback resolver (e.g. like a
		// InternalResourceViewResolver) so it needs to have low precedence
		resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
	}

}
