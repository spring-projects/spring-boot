/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Locale;

import javax.servlet.MultipartConfigElement;

import org.springframework.util.Assert;

/**
 * Factory that can be used to create a {@link MultipartConfigElement}. Size values can be
 * set using traditional {@literal long} values which are set in bytes or using more
 * readable {@literal String} variants that accept KB or MB suffixes, for example:
 *
 * <pre class="code">
 * factory.setMaxFileSize(&quot;10MB&quot;);
 * factory.setMaxRequestSize(&quot;100KB&quot;);
 * </pre>
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class MultipartConfigFactory {

	private String location;

	private long maxFileSize = -1;

	private long maxRequestSize = -1;

	private int fileSizeThreshold = 0;

	/**
	 * Sets the directory location where files will be stored.
	 * @param location the location
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * Sets the maximum size in bytes allowed for uploaded files.
	 * @param maxFileSize the maximum file size
	 * @see #setMaxFileSize(String)
	 */
	public void setMaxFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	/**
	 * Sets the maximum size allowed for uploaded files. Values can use the suffixed "MB"
	 * or "KB" to indicate a Megabyte or Kilobyte size.
	 * @param maxFileSize the maximum file size
	 * @see #setMaxFileSize(long)
	 */
	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSize = parseSize(maxFileSize);
	}

	/**
	 * Sets the maximum size allowed in bytes for multipart/form-data requests.
	 * @param maxRequestSize the maximum request size
	 * @see #setMaxRequestSize(String)
	 */
	public void setMaxRequestSize(long maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	/**
	 * Sets the maximum size allowed for multipart/form-data requests. Values can use the
	 * suffixed "MB" or "KB" to indicate a Megabyte or Kilobyte size.
	 * @param maxRequestSize the maximum request size
	 * @see #setMaxRequestSize(long)
	 */
	public void setMaxRequestSize(String maxRequestSize) {
		this.maxRequestSize = parseSize(maxRequestSize);
	}

	/**
	 * Sets the size threshold in bytes after which files will be written to disk.
	 * @param fileSizeThreshold the file size threshold
	 * @see #setFileSizeThreshold(String)
	 */
	public void setFileSizeThreshold(int fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

	/**
	 * Sets the size threshold after which files will be written to disk. Values can use
	 * the suffixed "MB" or "KB" to indicate a Megabyte or Kilobyte size.
	 * @param fileSizeThreshold the file size threshold
	 * @see #setFileSizeThreshold(int)
	 */
	public void setFileSizeThreshold(String fileSizeThreshold) {
		this.fileSizeThreshold = (int) parseSize(fileSizeThreshold);
	}

	private long parseSize(String size) {
		Assert.hasLength(size, "Size must not be empty");
		size = size.toUpperCase(Locale.ENGLISH);
		if (size.endsWith("KB")) {
			return Long.valueOf(size.substring(0, size.length() - 2)) * 1024;
		}
		if (size.endsWith("MB")) {
			return Long.valueOf(size.substring(0, size.length() - 2)) * 1024 * 1024;
		}
		return Long.valueOf(size);
	}

	/**
	 * Create a new {@link MultipartConfigElement} instance.
	 * @return the multipart config element
	 */
	public MultipartConfigElement createMultipartConfig() {
		return new MultipartConfigElement(this.location, this.maxFileSize,
				this.maxRequestSize, this.fileSizeThreshold);
	}

}
