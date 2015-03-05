/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.servlets.GzipFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * Properties for configuring {@link GzipFilter}.
 *
 * @author Andy Wilkinson
 * @since 1.2.2
 */
@ConfigurationProperties(prefix = "spring.http.gzip")
public class GzipFilterProperties {

	private final Map<String, String> initParameters = new HashMap<String, String>();

	/**
	 * Size of the output buffer in bytes.
	 */
	private Integer bufferSize;

	/**
	 * Minimum content length required for compression to occur.
	 */
	private Integer minGzipSize;

	/**
	 * Level used for deflate compression (0-9).
	 */
	private Integer deflateCompressionLevel;

	/**
	 * noWrap setting for deflate compression.
	 */
	private Boolean deflateNoWrap;

	/**
	 * Comma-separated list of HTTP methods for which compression is enabled.
	 */
	private List<HttpMethod> methods;

	/**
	 * Comma-separated list of MIME types which should be compressed.
	 */
	private List<MimeType> mimeTypes;

	/**
	 * Comma-separated list of user agents to exclude from compression. String.contains is
	 * used to determine a match against the request's User-Agent header.
	 */
	private String excludedAgents;

	/**
	 * Comma-separated list of regular expression patterns to control user agents excluded
	 * from compression.
	 */
	private String excludedAgentPatterns;

	/**
	 * Comma-separated list of paths to exclude from compression. Uses String.startsWith
	 * to determine a match against the request's path.
	 */
	private String excludedPaths;

	/**
	 * Comma-separated list of regular expression patterns to control the paths that are
	 * excluded from compression.
	 */
	private String excludedPathPatterns;

	/**
	 * Vary header sent on responses that may be compressed.
	 */
	private String vary;

	public GzipFilterProperties() {
		this.addInitParameter("checkGzExists", false);
	}

	public Integer getBufferSize() {
		return this.bufferSize;
	}

	public void setBufferSize(Integer bufferSize) {
		this.addInitParameter("bufferSize", bufferSize);
		this.bufferSize = bufferSize;
	}

	public Integer getMinGzipSize() {
		return this.minGzipSize;
	}

	public void setMinGzipSize(Integer minGzipSize) {
		this.addInitParameter("minGzipSize", minGzipSize);
		this.minGzipSize = minGzipSize;
	}

	public Integer getDeflateCompressionLevel() {
		return this.deflateCompressionLevel;
	}

	public void setDeflateCompressionLevel(Integer deflateCompressionLevel) {
		this.addInitParameter("deflateCompressionLevel", deflateCompressionLevel);
		this.deflateCompressionLevel = deflateCompressionLevel;
	}

	public Boolean getDeflateNoWrap() {
		return this.deflateNoWrap;
	}

	public void setDeflateNoWrap(Boolean deflateNoWrap) {
		this.addInitParameter("deflateNoWrap", deflateNoWrap);
		this.deflateNoWrap = deflateNoWrap;
	}

	public List<HttpMethod> getMethods() {
		return this.methods;
	}

	public void setMethods(List<HttpMethod> methods) {
		this.addInitParameter("methods",
				StringUtils.collectionToCommaDelimitedString(methods));
		this.methods = methods;
	}

	public List<MimeType> getMimeTypes() {
		return this.mimeTypes;
	}

	public void setMimeTypes(List<MimeType> mimeTypes) {
		this.addInitParameter("mimeTypes",
				StringUtils.collectionToCommaDelimitedString(mimeTypes));
		this.mimeTypes = mimeTypes;
	}

	public String getExcludedAgents() {
		return this.excludedAgents;
	}

	public void setExcludedAgents(String excludedAgents) {
		this.addInitParameter("excludedAgents", excludedAgents);
		this.excludedAgents = excludedAgents;
	}

	public String getExcludedAgentPatterns() {
		return this.excludedAgentPatterns;
	}

	public void setExcludedAgentPatterns(String excludedAgentPatterns) {
		this.addInitParameter("excludedAgentPatterns", excludedAgentPatterns);
		this.excludedAgentPatterns = excludedAgentPatterns;
	}

	public String getExcludedPaths() {
		return this.excludedPaths;
	}

	public void setExcludedPaths(String excludedPaths) {
		this.addInitParameter("excludedPaths", excludedPaths);
		this.excludedPaths = excludedPaths;
	}

	public String getExcludedPathPatterns() {
		return this.excludedPathPatterns;
	}

	public void setExcludedPathPatterns(String excludedPathPatterns) {
		this.addInitParameter("excludedPathPatterns", excludedPathPatterns);
		this.excludedPathPatterns = excludedPathPatterns;
	}

	public String getVary() {
		return this.vary;
	}

	public void setVary(String vary) {
		this.addInitParameter("vary", vary);
		this.vary = vary;
	}

	Map<String, String> getAsInitParameters() {
		return this.initParameters;
	}

	private void addInitParameter(String name, Integer value) {
		if (value != null) {
			this.initParameters.put(name, value.toString());
		}
	}

	private void addInitParameter(String name, Boolean value) {
		if (value != null) {
			this.initParameters.put(name, value.toString());
		}
	}

	private void addInitParameter(String name, String value) {
		if (value != null) {
			this.initParameters.put(name, value.toString());
		}
	}

}
