/*
 * Copyright 2012-present the original author or authors.
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
