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

package org.springframework.boot.micrometer.tracing.brave.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for tracing with Brave.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("management.brave.tracing")
public class BraveTracingProperties {

	/**
	 * Whether the propagation type and tracing backend support sharing the span ID
	 * between client and server spans. Requires B3 propagation and a compatible backend.
	 */
	private boolean spanJoiningSupported = false;

	public boolean isSpanJoiningSupported() {
		return this.spanJoiningSupported;
	}

	public void setSpanJoiningSupported(boolean spanJoiningSupported) {
		this.spanJoiningSupported = spanJoiningSupported;
	}

}
