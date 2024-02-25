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

package org.springframework.boot.autoconfigure.thymeleaf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.unit.DataSize;

/**
 * Properties for Thymeleaf.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Daniel Fernández
 * @author Kazuki Shimizu
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.thymeleaf")
public class ThymeleafProperties {

	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	public static final String DEFAULT_PREFIX = "classpath:/templates/";

	public static final String DEFAULT_SUFFIX = ".html";

	/**
	 * Whether to check that the template exists before rendering it.
	 */
	private boolean checkTemplate = true;

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
	 * Template mode to be applied to templates. See also Thymeleaf's TemplateMode enum.
	 */
	private String mode = "HTML";

	/**
	 * Template files encoding.
	 */
	private Charset encoding = DEFAULT_ENCODING;

	/**
	 * Whether to enable template caching.
	 */
	private boolean cache = true;

	/**
	 * Order of the template resolver in the chain. By default, the template resolver is
	 * first in the chain. Order start at 1 and should only be set if you have defined
	 * additional "TemplateResolver" beans.
	 */
	private Integer templateResolverOrder;

	/**
	 * Comma-separated list of view names (patterns allowed) that can be resolved.
	 */
	private String[] viewNames;

	/**
	 * Comma-separated list of view names (patterns allowed) that should be excluded from
	 * resolution.
	 */
	private String[] excludedViewNames;

	/**
	 * Enable the SpringEL compiler in SpringEL expressions.
	 */
	private boolean enableSpringElCompiler;

	/**
	 * Whether hidden form inputs acting as markers for checkboxes should be rendered
	 * before the checkbox element itself.
	 */
	private boolean renderHiddenMarkersBeforeCheckboxes = false;

	/**
	 * Whether to enable Thymeleaf view resolution for Web frameworks.
	 */
	private boolean enabled = true;

	private final Servlet servlet = new Servlet();

	private final Reactive reactive = new Reactive();

	/**
	 * Returns the current status of the enabled flag.
	 * @return {@code true} if the flag is enabled, {@code false} otherwise.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Sets the enabled status of the Thymeleaf properties.
	 * @param enabled the enabled status to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the value indicating whether the template should be checked for errors.
	 * @return {@code true} if the template should be checked for errors, {@code false}
	 * otherwise
	 */
	public boolean isCheckTemplate() {
		return this.checkTemplate;
	}

	/**
	 * Sets the flag indicating whether to check the template for errors.
	 * @param checkTemplate the flag indicating whether to check the template for errors
	 */
	public void setCheckTemplate(boolean checkTemplate) {
		this.checkTemplate = checkTemplate;
	}

	/**
	 * Returns the value indicating whether the template location should be checked.
	 * @return {@code true} if the template location should be checked, {@code false}
	 * otherwise
	 */
	public boolean isCheckTemplateLocation() {
		return this.checkTemplateLocation;
	}

	/**
	 * Sets the flag indicating whether to check the template location.
	 * @param checkTemplateLocation the flag indicating whether to check the template
	 * location
	 */
	public void setCheckTemplateLocation(boolean checkTemplateLocation) {
		this.checkTemplateLocation = checkTemplateLocation;
	}

	/**
	 * Returns the prefix used for resolving view names.
	 * @return the prefix used for resolving view names
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Sets the prefix for Thymeleaf templates.
	 * @param prefix the prefix to be set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Returns the suffix used for resolving view templates.
	 * @return the suffix used for resolving view templates
	 */
	public String getSuffix() {
		return this.suffix;
	}

	/**
	 * Sets the suffix to be appended to view names when resolving templates.
	 * @param suffix the suffix to be set
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Returns the mode of the Thymeleaf properties.
	 * @return the mode of the Thymeleaf properties
	 */
	public String getMode() {
		return this.mode;
	}

	/**
	 * Sets the mode for Thymeleaf.
	 * @param mode the mode to set
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Returns the encoding used by Thymeleaf.
	 * @return the encoding used by Thymeleaf
	 */
	public Charset getEncoding() {
		return this.encoding;
	}

	/**
	 * Sets the encoding for Thymeleaf properties.
	 * @param encoding the encoding to be set
	 */
	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}

	/**
	 * Returns a boolean value indicating whether caching is enabled.
	 * @return {@code true} if caching is enabled, {@code false} otherwise
	 */
	public boolean isCache() {
		return this.cache;
	}

	/**
	 * Sets the value indicating whether caching is enabled or not.
	 * @param cache the value indicating whether caching is enabled or not
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * Returns the order of the template resolver.
	 * @return the order of the template resolver
	 */
	public Integer getTemplateResolverOrder() {
		return this.templateResolverOrder;
	}

	/**
	 * Sets the order of the template resolver.
	 * @param templateResolverOrder the order of the template resolver
	 */
	public void setTemplateResolverOrder(Integer templateResolverOrder) {
		this.templateResolverOrder = templateResolverOrder;
	}

	/**
	 * Returns an array of excluded view names.
	 * @return the array of excluded view names
	 */
	public String[] getExcludedViewNames() {
		return this.excludedViewNames;
	}

	/**
	 * Sets the array of excluded view names.
	 * @param excludedViewNames the array of excluded view names to be set
	 */
	public void setExcludedViewNames(String[] excludedViewNames) {
		this.excludedViewNames = excludedViewNames;
	}

	/**
	 * Returns an array of view names.
	 * @return an array of view names
	 */
	public String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * Sets the array of view names.
	 * @param viewNames the array of view names to be set
	 */
	public void setViewNames(String[] viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * Returns the value indicating whether the Spring EL compiler is enabled.
	 * @return {@code true} if the Spring EL compiler is enabled, {@code false} otherwise
	 */
	public boolean isEnableSpringElCompiler() {
		return this.enableSpringElCompiler;
	}

	/**
	 * Sets the flag to enable or disable the Spring EL compiler.
	 * @param enableSpringElCompiler true to enable the Spring EL compiler, false to
	 * disable it
	 */
	public void setEnableSpringElCompiler(boolean enableSpringElCompiler) {
		this.enableSpringElCompiler = enableSpringElCompiler;
	}

	/**
	 * Returns the value indicating whether hidden markers should be rendered before
	 * checkboxes.
	 * @return {@code true} if hidden markers should be rendered before checkboxes,
	 * {@code false} otherwise
	 */
	public boolean isRenderHiddenMarkersBeforeCheckboxes() {
		return this.renderHiddenMarkersBeforeCheckboxes;
	}

	/**
	 * Sets the flag indicating whether hidden markers should be rendered before
	 * checkboxes.
	 * @param renderHiddenMarkersBeforeCheckboxes the flag indicating whether hidden
	 * markers should be rendered before checkboxes
	 */
	public void setRenderHiddenMarkersBeforeCheckboxes(boolean renderHiddenMarkersBeforeCheckboxes) {
		this.renderHiddenMarkersBeforeCheckboxes = renderHiddenMarkersBeforeCheckboxes;
	}

	/**
	 * Returns the Reactive object associated with this ThymeleafProperties instance.
	 * @return the Reactive object
	 */
	public Reactive getReactive() {
		return this.reactive;
	}

	/**
	 * Returns the servlet associated with this ThymeleafProperties instance.
	 * @return the servlet associated with this ThymeleafProperties instance
	 */
	public Servlet getServlet() {
		return this.servlet;
	}

	/**
	 * Servlet class.
	 */
	public static class Servlet {

		/**
		 * Content-Type value written to HTTP responses.
		 */
		private MimeType contentType = MimeType.valueOf("text/html");

		/**
		 * Whether Thymeleaf should start writing partial output as soon as possible or
		 * buffer until template processing is finished.
		 */
		private boolean producePartialOutputWhileProcessing = true;

		/**
		 * Returns the content type of the response.
		 * @return the content type of the response
		 */
		public MimeType getContentType() {
			return this.contentType;
		}

		/**
		 * Sets the content type of the response.
		 * @param contentType the MIME type of the content
		 */
		public void setContentType(MimeType contentType) {
			this.contentType = contentType;
		}

		/**
		 * Returns a boolean value indicating whether the servlet is configured to produce
		 * partial output while processing.
		 * @return {@code true} if the servlet is configured to produce partial output
		 * while processing, {@code false} otherwise.
		 */
		public boolean isProducePartialOutputWhileProcessing() {
			return this.producePartialOutputWhileProcessing;
		}

		/**
		 * Sets whether to produce partial output while processing.
		 * @param producePartialOutputWhileProcessing true to produce partial output while
		 * processing, false otherwise
		 */
		public void setProducePartialOutputWhileProcessing(boolean producePartialOutputWhileProcessing) {
			this.producePartialOutputWhileProcessing = producePartialOutputWhileProcessing;
		}

	}

	/**
	 * Reactive class.
	 */
	public static class Reactive {

		/**
		 * Maximum size of data buffers used for writing to the response. Templates will
		 * execute in CHUNKED mode by default if this is set.
		 */
		private DataSize maxChunkSize = DataSize.ofBytes(0);

		/**
		 * Media types supported by the view technology.
		 */
		private List<MediaType> mediaTypes;

		/**
		 * Comma-separated list of view names (patterns allowed) that should be executed
		 * in FULL mode even if a max chunk size is set.
		 */
		private String[] fullModeViewNames;

		/**
		 * Comma-separated list of view names (patterns allowed) that should be the only
		 * ones executed in CHUNKED mode when a max chunk size is set.
		 */
		private String[] chunkedModeViewNames;

		/**
		 * Returns the list of media types supported by this Reactive instance.
		 * @return the list of media types
		 */
		public List<MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		/**
		 * Sets the list of media types for the Reactive class.
		 * @param mediaTypes the list of media types to be set
		 */
		public void setMediaTypes(List<MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

		/**
		 * Returns the maximum chunk size.
		 * @return the maximum chunk size
		 */
		public DataSize getMaxChunkSize() {
			return this.maxChunkSize;
		}

		/**
		 * Sets the maximum chunk size for the Reactive class.
		 * @param maxChunkSize the maximum chunk size to be set
		 */
		public void setMaxChunkSize(DataSize maxChunkSize) {
			this.maxChunkSize = maxChunkSize;
		}

		/**
		 * Returns an array of full mode view names.
		 * @return an array of full mode view names
		 */
		public String[] getFullModeViewNames() {
			return this.fullModeViewNames;
		}

		/**
		 * Sets the names of the views to be displayed in full mode.
		 * @param fullModeViewNames an array of strings representing the names of the
		 * views to be displayed in full mode
		 */
		public void setFullModeViewNames(String[] fullModeViewNames) {
			this.fullModeViewNames = fullModeViewNames;
		}

		/**
		 * Returns an array of chunked mode view names.
		 * @return the array of chunked mode view names
		 */
		public String[] getChunkedModeViewNames() {
			return this.chunkedModeViewNames;
		}

		/**
		 * Sets the array of chunked mode view names.
		 * @param chunkedModeViewNames the array of chunked mode view names to be set
		 */
		public void setChunkedModeViewNames(String[] chunkedModeViewNames) {
			this.chunkedModeViewNames = chunkedModeViewNames;
		}

	}

}
