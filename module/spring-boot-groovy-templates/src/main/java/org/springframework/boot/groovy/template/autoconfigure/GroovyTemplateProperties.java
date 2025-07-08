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

package org.springframework.boot.groovy.template.autoconfigure;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import groovy.text.markup.BaseTemplate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Groovy
 * templates.
 *
 * @author Dave Syer
 * @author Marten Deinum
 * @since 4.0.0
 */
@ConfigurationProperties("spring.groovy.template")
public class GroovyTemplateProperties {

	public static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:/templates/";

	public static final String DEFAULT_PREFIX = "";

	public static final String DEFAULT_SUFFIX = ".tpl";

	public static final String DEFAULT_REQUEST_CONTEXT_ATTRIBUTE = "spring";

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
	private String requestContextAttribute = DEFAULT_REQUEST_CONTEXT_ATTRIBUTE;

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
	 * Whether models that are assignable to CharSequence are escaped automatically.
	 */
	private boolean autoEscape;

	/**
	 * Whether indents are rendered automatically.
	 */
	private boolean autoIndent;

	/**
	 * String used for auto-indents.
	 */
	private String autoIndentString;

	/**
	 * Whether new lines are rendered automatically.
	 */
	private boolean autoNewLine;

	/**
	 * Template base class.
	 */
	private Class<? extends BaseTemplate> baseTemplateClass = BaseTemplate.class;

	/**
	 * Encoding used to write the declaration heading.
	 */
	private String declarationEncoding;

	/**
	 * Whether elements without a body should be written expanded (&lt;br&gt;&lt;/br&gt;)
	 * or not (&lt;br/&gt;).
	 */
	private boolean expandEmptyElements;

	/**
	 * Default locale for template resolution.
	 */
	private Locale locale;

	/**
	 * String used to write a new line. Defaults to the system's line separator.
	 */
	private String newLineString;

	/**
	 * Template path.
	 */
	private String resourceLoaderPath = DEFAULT_RESOURCE_LOADER_PATH;

	/**
	 * Whether attributes should use double quotes.
	 */
	private boolean useDoubleQuotes;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
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

	public boolean isAutoEscape() {
		return this.autoEscape;
	}

	public void setAutoEscape(boolean autoEscape) {
		this.autoEscape = autoEscape;
	}

	public boolean isAutoIndent() {
		return this.autoIndent;
	}

	public void setAutoIndent(boolean autoIndent) {
		this.autoIndent = autoIndent;
	}

	public String getAutoIndentString() {
		return this.autoIndentString;
	}

	public void setAutoIndentString(String autoIndentString) {
		this.autoIndentString = autoIndentString;
	}

	public boolean isAutoNewLine() {
		return this.autoNewLine;
	}

	public void setAutoNewLine(boolean autoNewLine) {
		this.autoNewLine = autoNewLine;
	}

	public Class<? extends BaseTemplate> getBaseTemplateClass() {
		return this.baseTemplateClass;
	}

	public void setBaseTemplateClass(Class<? extends BaseTemplate> baseTemplateClass) {
		this.baseTemplateClass = baseTemplateClass;
	}

	public String getDeclarationEncoding() {
		return this.declarationEncoding;
	}

	public void setDeclarationEncoding(String declarationEncoding) {
		this.declarationEncoding = declarationEncoding;
	}

	public boolean isExpandEmptyElements() {
		return this.expandEmptyElements;
	}

	public void setExpandEmptyElements(boolean expandEmptyElements) {
		this.expandEmptyElements = expandEmptyElements;
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public String getNewLineString() {
		return this.newLineString;
	}

	public void setNewLineString(String newLineString) {
		this.newLineString = newLineString;
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public boolean isUseDoubleQuotes() {
		return this.useDoubleQuotes;
	}

	public void setUseDoubleQuotes(boolean useDoubleQuotes) {
		this.useDoubleQuotes = useDoubleQuotes;
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
