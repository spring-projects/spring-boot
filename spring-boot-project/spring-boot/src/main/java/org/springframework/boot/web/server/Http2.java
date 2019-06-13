/*
 * Copyright 2012-2018 the original author or authors.
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

/**
 * Simple server-independent abstraction for HTTP/2 configuration.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class Http2 {

	private boolean enabled = false;

	/**
	 * Return whether to enable HTTP/2 support, if the current environment supports it.
	 * @return {@code true} to enable HTTP/2 support
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
