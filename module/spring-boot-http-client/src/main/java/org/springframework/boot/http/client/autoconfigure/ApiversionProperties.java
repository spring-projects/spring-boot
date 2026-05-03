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

package org.springframework.boot.http.client.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Name;

/**
 * API Version properties for both reactive and imperative HTTP Clients.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ApiversionProperties {

	/**
	 * Default version that should be used for each request.
	 */
	@Name("default")
	private @Nullable String defaultVersion;

	/**
	 * How version details should be inserted into requests.
	 */
	private final Insert insert = new Insert();

	public @Nullable String getDefaultVersion() {
		return this.defaultVersion;
	}

	public void setDefaultVersion(@Nullable String defaultVersion) {
		this.defaultVersion = defaultVersion;
	}

	public Insert getInsert() {
		return this.insert;
	}

	public static class Insert {

		/**
		 * Insert the version into a header with the given name.
		 */
		private @Nullable String header;

		/**
		 * Insert the version into a query parameter with the given name.
		 */
		private @Nullable String queryParameter;

		/**
		 * Insert the version into a path segment at the given index.
		 */
		private @Nullable Integer pathSegment;

		/**
		 * Insert the version into a media type parameter with the given name.
		 */
		private @Nullable String mediaTypeParameter;

		public @Nullable String getHeader() {
			return this.header;
		}

		public void setHeader(@Nullable String header) {
			this.header = header;
		}

		public @Nullable String getQueryParameter() {
			return this.queryParameter;
		}

		public void setQueryParameter(@Nullable String queryParameter) {
			this.queryParameter = queryParameter;
		}

		public @Nullable Integer getPathSegment() {
			return this.pathSegment;
		}

		public void setPathSegment(@Nullable Integer pathSegment) {
			this.pathSegment = pathSegment;
		}

		public @Nullable String getMediaTypeParameter() {
			return this.mediaTypeParameter;
		}

		public void setMediaTypeParameter(@Nullable String mediaTypeParameter) {
			this.mediaTypeParameter = mediaTypeParameter;
		}

	}

}
