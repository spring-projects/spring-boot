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

package org.springframework.boot.test.autoconfigure.restdocs;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring REST Docs.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @since 2.0.0
 */
@ConfigurationProperties("spring.test.restdocs")
public class RestDocsProperties {

	/**
	 * The URI scheme for to use (for example http).
	 */
	private @Nullable String uriScheme;

	/**
	 * The URI host to use.
	 */
	private @Nullable String uriHost;

	/**
	 * The URI port to use.
	 */
	private @Nullable Integer uriPort;

	public @Nullable String getUriScheme() {
		return this.uriScheme;
	}

	public void setUriScheme(@Nullable String uriScheme) {
		this.uriScheme = uriScheme;
	}

	public @Nullable String getUriHost() {
		return this.uriHost;
	}

	public void setUriHost(@Nullable String uriHost) {
		this.uriHost = uriHost;
	}

	public @Nullable Integer getUriPort() {
		return this.uriPort;
	}

	public void setUriPort(@Nullable Integer uriPort) {
		this.uriPort = uriPort;
	}

}
