/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import jakarta.servlet.MultipartConfigElement;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;

/**
 * Properties to be used in configuring a {@link MultipartConfigElement}.
 * <ul>
 * <li>{@link #getLocation() location} specifies the directory where uploaded files will
 * be stored. When not specified, a temporary directory will be used.</li>
 * <li>{@link #getMaxFileSize() max-file-size} specifies the maximum size permitted for
 * uploaded files. The default is 1MB</li>
 * <li>{@link #getMaxRequestSize() max-request-size} specifies the maximum size allowed
 * for {@literal multipart/form-data} requests. The default is 10MB.</li>
 * <li>{@link #getFileSizeThreshold() file-size-threshold} specifies the size threshold
 * after which files will be written to disk. The default is 0.</li>
 * </ul>
 * <p>
 * These properties are ultimately passed to {@link MultipartConfigFactory} which means
 * you may specify numeric values using {@literal long} values or using more readable
 * {@link DataSize} variants.
 *
 * @author Josh Long
 * @author Toshiaki Maki
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.servlet.multipart", ignoreUnknownFields = false)
public class MultipartProperties {

	/**
	 * Whether to enable support of multipart uploads.
	 */
	private boolean enabled = true;

	/**
	 * Intermediate location of uploaded files.
	 */
	private String location;

	/**
	 * Max file size.
	 */
	private DataSize maxFileSize = DataSize.ofMegabytes(1);

	/**
	 * Max request size.
	 */
	private DataSize maxRequestSize = DataSize.ofMegabytes(10);

	/**
	 * Threshold after which files are written to disk.
	 */
	private DataSize fileSizeThreshold = DataSize.ofBytes(0);

	/**
	 * Whether to resolve the multipart request lazily at the time of file or parameter
	 * access.
	 */
	private boolean resolveLazily = false;

	/**
	 * Whether to resolve the multipart request strictly complying with the Servlet
	 * specification, only to be used for "multipart/form-data" requests.
	 */
	private boolean strictServletCompliance = false;

	/**
     * Returns the value of the enabled property.
     *
     * @return true if the property is enabled, false otherwise.
     */
    public boolean getEnabled() {
		return this.enabled;
	}

	/**
     * Sets the enabled status of the MultipartProperties.
     * 
     * @param enabled the enabled status to be set
     */
    public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
     * Returns the location of the MultipartProperties.
     * 
     * @return the location of the MultipartProperties
     */
    public String getLocation() {
		return this.location;
	}

	/**
     * Sets the location of the multipart file.
     * 
     * @param location the location of the multipart file
     */
    public void setLocation(String location) {
		this.location = location;
	}

	/**
     * Returns the maximum file size allowed for multipart file uploads.
     *
     * @return the maximum file size allowed
     */
    public DataSize getMaxFileSize() {
		return this.maxFileSize;
	}

	/**
     * Sets the maximum file size for multipart requests.
     * 
     * @param maxFileSize the maximum file size to be set
     */
    public void setMaxFileSize(DataSize maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	/**
     * Returns the maximum request size allowed for multipart requests.
     *
     * @return the maximum request size as a DataSize object
     */
    public DataSize getMaxRequestSize() {
		return this.maxRequestSize;
	}

	/**
     * Sets the maximum request size for multipart requests.
     * 
     * @param maxRequestSize the maximum request size to be set
     */
    public void setMaxRequestSize(DataSize maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	/**
     * Returns the file size threshold for multipart file uploads.
     * 
     * @return the file size threshold
     */
    public DataSize getFileSizeThreshold() {
		return this.fileSizeThreshold;
	}

	/**
     * Sets the file size threshold for multipart requests.
     * 
     * @param fileSizeThreshold the file size threshold to be set
     */
    public void setFileSizeThreshold(DataSize fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

	/**
     * Returns a boolean value indicating whether the resolving is done lazily.
     *
     * @return {@code true} if the resolving is done lazily, {@code false} otherwise.
     */
    public boolean isResolveLazily() {
		return this.resolveLazily;
	}

	/**
     * Sets the flag indicating whether to resolve lazily.
     * 
     * @param resolveLazily the flag indicating whether to resolve lazily
     */
    public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
     * Returns a boolean value indicating whether the strict servlet compliance is enabled.
     * 
     * @return true if strict servlet compliance is enabled, false otherwise
     */
    public boolean isStrictServletCompliance() {
		return this.strictServletCompliance;
	}

	/**
     * Sets the flag indicating whether strict servlet compliance is enabled or not.
     * 
     * @param strictServletCompliance the flag indicating whether strict servlet compliance is enabled or not
     */
    public void setStrictServletCompliance(boolean strictServletCompliance) {
		this.strictServletCompliance = strictServletCompliance;
	}

	/**
	 * Create a new {@link MultipartConfigElement} using the properties.
	 * @return a new {@link MultipartConfigElement} configured using there properties
	 */
	public MultipartConfigElement createMultipartConfig() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.fileSizeThreshold).to(factory::setFileSizeThreshold);
		map.from(this.location).whenHasText().to(factory::setLocation);
		map.from(this.maxRequestSize).to(factory::setMaxRequestSize);
		map.from(this.maxFileSize).to(factory::setMaxFileSize);
		return factory.createMultipartConfig();
	}

}
