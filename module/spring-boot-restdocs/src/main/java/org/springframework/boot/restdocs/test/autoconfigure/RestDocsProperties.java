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

package org.springframework.boot.restdocs.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring REST Docs.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @since 4.0.0
 */
@ConfigurationProperties("spring.restdocs")
public class RestDocsProperties {

	private final Uri uri = new Uri();

	public Uri getUri() {
		return this.uri;
	}

	public static class Uri {

		/**
		 * The URI scheme to use (for example http).
		 */
		private @Nullable String scheme;

		/**
		 * The URI host to use.
		 */
		private @Nullable String host;

		/**
		 * The URI port to use.
		 */
		private @Nullable Integer port;

		public @Nullable String getScheme() {
			return this.scheme;
		}

		public void setScheme(@Nullable String scheme) {
			this.scheme = scheme;
		}

		public @Nullable String getHost() {
			return this.host;
		}

		public void setHost(@Nullable String host) {
			this.host = host;
		}

		public @Nullable Integer getPort() {
			return this.port;
		}

		public void setPort(@Nullable Integer port) {
			this.port = port;
		}

	}

}
