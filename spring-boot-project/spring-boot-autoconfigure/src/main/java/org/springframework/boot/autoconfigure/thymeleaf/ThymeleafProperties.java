/*
 * Copyright 2012-2019 the original author or authors.
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

	public boolean isEnableSpringElCompiler() {
		return this.enableSpringElCompiler;
	}

	public void setEnableSpringElCompiler(boolean enableSpringElCompiler) {
		this.enableSpringElCompiler = enableSpringElCompiler;
	}

	public boolean isRenderHiddenMarkersBeforeCheckboxes() {
		return this.renderHiddenMarkersBeforeCheckboxes;
	}

	public void setRenderHiddenMarkersBeforeCheckboxes(boolean renderHiddenMarkersBeforeCheckboxes) {
		this.renderHiddenMarkersBeforeCheckboxes = renderHiddenMarkersBeforeCheckboxes;
	}

	public Reactive getReactive() {
		return this.reactive;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

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

		public MimeType getContentType() {
			return this.contentType;
		}

		public void setContentType(MimeType contentType) {
			this.contentType = contentType;
		}

		public boolean isProducePartialOutputWhileProcessing() {
			return this.producePartialOutputWhileProcessing;
		}

		public void setProducePartialOutputWhileProcessing(boolean producePartialOutputWhileProcessing) {
			this.producePartialOutputWhileProcessing = producePartialOutputWhileProcessing;
		}

	}

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

		public List<MediaType> getMediaTypes() {
			return this.mediaTypes;
		}

		public void setMediaTypes(List<MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
		}

		public DataSize getMaxChunkSize() {
			return this.maxChunkSize;
		}

		public void setMaxChunkSize(DataSize maxChunkSize) {
			this.maxChunkSize = maxChunkSize;
		}

		public String[] getFullModeViewNames() {
			return this.fullModeViewNames;
		}

		public void setFullModeViewNames(String[] fullModeViewNames) {
			this.fullModeViewNames = fullModeViewNames;
		}

		public String[] getChunkedModeViewNames() {
			return this.chunkedModeViewNames;
		}

		public void setChunkedModeViewNames(String[] chunkedModeViewNames) {
			this.chunkedModeViewNames = chunkedModeViewNames;
		}

	}

}
