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

package org.springframework.boot.web.server;

/**
 * Simple server-independent abstraction for compression configuration.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class Compression {

	/**
	 * Whether response compression is enabled.
	 */
	private boolean enabled = false;

	/**
	 * Comma-separated list of MIME types that should be compressed.
	 */
	private String[] mimeTypes = new String[] { "text/html", "text/xml", "text/plain",
			"text/css", "text/javascript", "application/javascript", "application/json",
			"application/xml" };

	/**
	 * Comma-separated list of user agents for which responses should not be compressed.
	 */
	private String[] excludedUserAgents = null;

	/**
	 * Minimum "Content-Length" value that is required for compression to be performed.
	 */
	private int minResponseSize = 2048;

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String[] getMimeTypes() {
		return this.mimeTypes;
	}

	public void setMimeTypes(String[] mimeTypes) {
		this.mimeTypes = mimeTypes;
	}

	public int getMinResponseSize() {
		return this.minResponseSize;
	}

	public void setMinResponseSize(int minSize) {
		this.minResponseSize = minSize;
	}

	public String[] getExcludedUserAgents() {
		return this.excludedUserAgents;
	}

	public void setExcludedUserAgents(String[] excludedUserAgents) {
		this.excludedUserAgents = excludedUserAgents;
	}

}
