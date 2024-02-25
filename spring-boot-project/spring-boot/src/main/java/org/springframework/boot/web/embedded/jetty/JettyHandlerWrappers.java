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

package org.springframework.boot.web.embedded.jetty;

import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.Callback;

import org.springframework.boot.web.server.Compression;

/**
 * Jetty {@code HandlerWrapper} static factory.
 *
 * @author Brian Clozel
 */
final class JettyHandlerWrappers {

	/**
	 * Private constructor for the JettyHandlerWrappers class.
	 */
	private JettyHandlerWrappers() {
	}

	/**
	 * Creates a wrapper for the GzipHandler with the specified compression settings.
	 * @param compression the Compression object containing the compression settings
	 * @return a Handler.Wrapper object representing the GzipHandler with the specified
	 * compression settings
	 */
	static Handler.Wrapper createGzipHandlerWrapper(Compression compression) {
		GzipHandler handler = new GzipHandler();
		handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
		handler.setIncludedMimeTypes(compression.getMimeTypes());
		for (HttpMethod httpMethod : HttpMethod.values()) {
			handler.addIncludedMethods(httpMethod.name());
		}
		return handler;
	}

	/**
	 * Creates a {@link Handler.Wrapper} object that adds a server header to the HTTP
	 * response.
	 * @param header the value of the server header to be added
	 * @return a {@link Handler.Wrapper} object that adds the specified server header
	 */
	static Handler.Wrapper createServerHeaderHandlerWrapper(String header) {
		return new ServerHeaderHandler(header);
	}

	/**
	 * {@link Handler.Wrapper} to add a custom {@code server} header.
	 */
	private static class ServerHeaderHandler extends Handler.Wrapper {

		private static final String SERVER_HEADER = "server";

		private final String value;

		/**
		 * Constructs a new ServerHeaderHandler with the specified value.
		 * @param value the value to be set for the ServerHeaderHandler
		 */
		ServerHeaderHandler(String value) {
			this.value = value;
		}

		/**
		 * Handles the request and response by adding the server header to the response
		 * headers if it is not already present.
		 * @param request the request object
		 * @param response the response object
		 * @param callback the callback object
		 * @return true if the request and response are handled successfully, false
		 * otherwise
		 * @throws Exception if an error occurs while handling the request and response
		 */
		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			Mutable headers = response.getHeaders();
			if (!headers.contains(SERVER_HEADER)) {
				headers.add(SERVER_HEADER, this.value);
			}
			return super.handle(request, response, callback);
		}

	}

}
