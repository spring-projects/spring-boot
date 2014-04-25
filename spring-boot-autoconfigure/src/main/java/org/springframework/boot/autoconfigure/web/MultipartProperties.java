/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Properties to be used in configuring a {@link MultipartConfigElement}.
 * <ul>
 * <li>{@literal multipart.location} specifies the directory where files will be stored.
 * The default is "". A common value is to use the system's temporary directory, which can
 * be obtained.</li>
 * <li>{@literal multipart.maxFileSize} specifies the maximum size permitted for uploaded
 * files. The default is 1Mb.</li>
 * <li>
 * {@literal multipart.maxRequestSize} specifies the maximum size allowed for
 * {@literal multipart/form-data} requests. The default is 10Mb</li>
 * <li>
 * {@literal multipart.fileSizeThreshold} specifies the size threshold after which files
 * will be written to disk. Default is 0, which means that the file will be written to
 * disk immediately.</li>
 * </ul>
 * <p>
 * These properties are ultimately passed through
 * {@link org.springframework.boot.context.embedded.MultipartConfigFactory} which means
 * you may specify the values using {@literal long} values or using more readable
 * {@literal String} variants that accept {@literal Kb} or {@literal Mb} suffixes.
 * 
 * @author Josh Long
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "multipart", ignoreUnknownFields = false)
public class MultipartProperties {

	private String location;

	private String maxFileSize = "1Mb";

	private String maxRequestSize = "10Mb";

	private String fileSizeThreshold = "0";

	public String getMaxFileSize() {
		return this.maxFileSize;
	}

	public String getMaxRequestSize() {
		return this.maxRequestSize;
	}

	public String getFileSizeThreshold() {
		return this.fileSizeThreshold;
	}

	public String getLocation() {
		return this.location;
	}

	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public void setMaxRequestSize(String maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setFileSizeThreshold(String fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

	/**
	 * Create a new {@link MultipartConfigElement} using the
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
