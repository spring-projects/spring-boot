/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet;

import jakarta.servlet.MultipartConfigElement;
import org.jspecify.annotations.Nullable;

import org.springframework.util.unit.DataSize;

/**
 * Factory that can be used to create a {@link MultipartConfigElement}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class MultipartConfigFactory {

	private @Nullable String location;

	private @Nullable DataSize maxFileSize;

	private @Nullable DataSize maxRequestSize;

	private @Nullable DataSize fileSizeThreshold;

	/**
	 * Sets the directory location where files will be stored.
	 * @param location the location
	 */
	public void setLocation(@Nullable String location) {
		this.location = location;
	}

	/**
	 * Sets the maximum {@link DataSize size} allowed for uploaded files.
	 * @param maxFileSize the maximum file size
	 */
	public void setMaxFileSize(@Nullable DataSize maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	/**
	 * Sets the maximum {@link DataSize} allowed for multipart/form-data requests.
	 * @param maxRequestSize the maximum request size
	 */
	public void setMaxRequestSize(@Nullable DataSize maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}

	/**
	 * Sets the {@link DataSize size} threshold after which files will be written to disk.
	 * @param fileSizeThreshold the file size threshold
	 */
	public void setFileSizeThreshold(@Nullable DataSize fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}

	/**
	 * Create a new {@link MultipartConfigElement} instance.
	 * @return the multipart config element
	 */
	public MultipartConfigElement createMultipartConfig() {
		long maxFileSizeBytes = convertToBytes(this.maxFileSize, -1);
		long maxRequestSizeBytes = convertToBytes(this.maxRequestSize, -1);
		long fileSizeThresholdBytes = convertToBytes(this.fileSizeThreshold, 0);
		return new MultipartConfigElement(this.location, maxFileSizeBytes, maxRequestSizeBytes,
				(int) fileSizeThresholdBytes);
	}

	/**
	 * Return the amount of bytes from the specified {@link DataSize size}. If the size is
	 * {@code null} or negative, returns {@code defaultValue}.
	 * @param size the data size to handle
	 * @param defaultValue the default value if the size is {@code null} or negative
	 * @return the amount of bytes to use
	 */
	private long convertToBytes(@Nullable DataSize size, int defaultValue) {
		if (size != null && !size.isNegative()) {
			return size.toBytes();
		}
		return defaultValue;
	}

}
