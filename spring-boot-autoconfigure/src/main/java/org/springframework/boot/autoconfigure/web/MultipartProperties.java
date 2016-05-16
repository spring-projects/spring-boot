/*
 * Copyright 2012-2016 the original author or authors.
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

import javax.servlet.MultipartConfigElement;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.StringUtils;

/**
 * Properties to be used in configuring a {@link MultipartConfigElement}.
 * <ul>
 * <li>{@link #getLocation() location} specifies the directory where files will be stored.
 * The default is "". A common value is to use the system's temporary directory, which can
 * be obtained.</li>
 * <li>{@link #getMaxFileSize() max-file-size} specifies the maximum size permitted for
 * uploaded files. The default is 1Mb.</li>
 * <li>{@link #getMaxRequestSize() max-request-size} specifies the maximum size allowed
 * for {@literal multipart/form-data} requests. The default is 10Mb</li>
 * <li>{@link #getFileSizeThreshold() file-size-threshold} specifies the size threshold
 * after which files will be written to disk. Default is 0, which means that the file will
 * be written to disk immediately.</li>
 * </ul>
 * <p>
 * These properties are ultimately passed through {@link MultipartConfigFactory} which
 * means you may specify the values using {@literal long} values or using more readable
 * {@literal String} variants that accept {@literal Kb} or {@literal Mb} suffixes.
 *
 * @author Josh Long
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.http.multipart", ignoreUnknownFields = false)
public class MultipartProperties {

	/**
	 * Enable support of multi-part uploads.
	 */
	private boolean enabled = true;

	/**
	 * Intermediate location of uploaded files.
	 */
	private String location;

	/**
	 * Max file size. Values can use the suffixed "MB" or "KB" to indicate a Megabyte or
	 * Kilobyte size.
	 */
	private String maxFileSize = "1Mb";

	/**
	 * Max request size. Values can use the suffixed "MB" or "KB" to indicate a Megabyte
	 * or Kilobyte size.
	 */
	private String maxRequestSize = "10Mb";

	/**
	 * Threshold after which files will be written to disk. Values can use the suffixed
	 * "MB" or "KB" to indicate a Megabyte or Kilobyte size.
	 */
	private String fileSizeThreshold = "0";

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getMaxFileSize() {
		return this.maxFileSize;
	}

	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public String getMaxRequestSize() {
		return this.maxRequestSize;
	}

	public void setMaxRequestSize(String maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	public String getFileSizeThreshold() {
		return this.fileSizeThreshold;
	}

	public void setFileSizeThreshold(String fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

	/**
	 * Create a new {@link MultipartConfigElement} using the properties.
	 * @return a new {@link MultipartConfigElement} configured using there properties
	 */
	public MultipartConfigElement createMultipartConfig() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		if (StringUtils.hasText(this.fileSizeThreshold)) {
			factory.setFileSizeThreshold(this.fileSizeThreshold);
		}
		if (StringUtils.hasText(this.location)) {
			factory.setLocation(this.location);
		}
		if (StringUtils.hasText(this.maxRequestSize)) {
			factory.setMaxRequestSize(this.maxRequestSize);
		}
		if (StringUtils.hasText(this.maxFileSize)) {
			factory.setMaxFileSize(this.maxFileSize);
		}
		return factory.createMultipartConfig();
	}

}
