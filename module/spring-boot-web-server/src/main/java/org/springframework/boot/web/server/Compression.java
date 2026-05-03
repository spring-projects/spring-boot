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

package org.springframework.boot.web.server;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Simple server-independent abstraction for compression configuration.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class Compression {

	/**
	 * Whether response compression is enabled.
	 */
	private boolean enabled;

	/**
	 * Comma-separated list of MIME types that should be compressed.
	 */
	private String[] mimeTypes = new String[] { "text/html", "text/xml", "text/plain", "text/css", "text/javascript",
			"application/javascript", "application/json", "application/xml" };

	/**
	 * Comma-separated list of additional MIME types that should be compressed.
	 */
	private String[] additionalMimeTypes = new String[0];

	/**
	 * Comma-separated list of user agents for which responses should not be compressed.
	 */
	private String @Nullable [] excludedUserAgents;

	/**
	 * Minimum "Content-Length" value that is required for compression to be performed.
	 */
	private DataSize minResponseSize = DataSize.ofKilobytes(2);

	/**
	 * Return whether response compression is enabled.
	 * @return {@code true} if response compression is enabled
	 */
	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Return the MIME types that should be compressed.
	 * @return the MIME types that should be compressed
	 */
	public String[] getMimeTypes() {
		return this.mimeTypes;
	}

	public void setMimeTypes(String[] mimeTypes) {
		this.mimeTypes = mimeTypes;
	}

	/**
	 * Return the MIME types that should be compressed, including any additional types.
	 * @return the MIME types that should be compressed
	 */
	public String[] getAllMimeTypes() {
		String[] combined = StringUtils.concatenateStringArrays(this.mimeTypes, this.additionalMimeTypes);
		return (combined != null) ? combined : this.mimeTypes;
	}

	public String[] getAdditionalMimeTypes() {
		return this.additionalMimeTypes;
	}

	public void setAdditionalMimeTypes(String[] additionalMimeTypes) {
		this.additionalMimeTypes = additionalMimeTypes;
	}

	public String @Nullable [] getExcludedUserAgents() {
		return this.excludedUserAgents;
	}

	public void setExcludedUserAgents(String @Nullable [] excludedUserAgents) {
		this.excludedUserAgents = excludedUserAgents;
	}

	/**
	 * Return the minimum "Content-Length" value that is required for compression to be
	 * performed.
	 * @return the minimum content size in bytes that is required for compression
	 */
	public DataSize getMinResponseSize() {
		return this.minResponseSize;
	}

	public void setMinResponseSize(DataSize minSize) {
		this.minResponseSize = minSize;
	}

}
