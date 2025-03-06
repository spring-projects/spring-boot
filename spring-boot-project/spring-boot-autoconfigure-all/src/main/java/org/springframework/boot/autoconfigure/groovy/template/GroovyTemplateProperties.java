/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import java.util.Locale;

import groovy.text.markup.BaseTemplate;

import org.springframework.boot.autoconfigure.template.AbstractTemplateViewResolverProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Groovy
 * templates.
 *
 * @author Dave Syer
 * @author Marten Deinum
 * @since 1.1.0
 */
@ConfigurationProperties("spring.groovy.template")
public class GroovyTemplateProperties extends AbstractTemplateViewResolverProperties {

	public static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:/templates/";

	public static final String DEFAULT_PREFIX = "";

	public static final String DEFAULT_SUFFIX = ".tpl";

	public static final String DEFAULT_REQUEST_CONTEXT_ATTRIBUTE = "spring";

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

	public GroovyTemplateProperties() {
		super(DEFAULT_PREFIX, DEFAULT_SUFFIX);
		setRequestContextAttribute(DEFAULT_REQUEST_CONTEXT_ATTRIBUTE);
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

}
