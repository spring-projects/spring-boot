/*
 * Copyright 2012-2021 the original author or authors.
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import org.springframework.boot.web.server.Compression;

/**
 * Jetty {@code HandlerWrapper} static factory.
 *
 * @author Brian Clozel
 */
final class JettyHandlerWrappers {

	private JettyHandlerWrappers() {
	}

	static HandlerWrapper createGzipHandlerWrapper(Compression compression) {
		GzipHandler handler = new GzipHandler();
		handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
		handler.setIncludedMimeTypes(compression.getMimeTypes());
		for (HttpMethod httpMethod : HttpMethod.values()) {
			handler.addIncludedMethods(httpMethod.name());
		}
		if (compression.getExcludedUserAgents() != null) {
			try {
				handler.setExcludedAgentPatterns(compression.getExcludedUserAgents());
			}
			catch (NoSuchMethodError ex) {
				// Jetty 10 does not support User-Agent-based exclusions
			}
		}
		return handler;
	}

	static HandlerWrapper createServerHeaderHandlerWrapper(String header) {
		return new ServerHeaderHandler(header);
	}

	/**
	 * {@link HandlerWrapper} to add a custom {@code server} header.
	 */
	private static class ServerHeaderHandler extends HandlerWrapper {

		private static final String SERVER_HEADER = "server";

		private final String value;

		ServerHeaderHandler(String value) {
			this.value = value;
		}

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			if (!response.getHeaderNames().contains(SERVER_HEADER)) {
				response.setHeader(SERVER_HEADER, this.value);
			}
			super.handle(target, baseRequest, request, response);
		}

	}

}
