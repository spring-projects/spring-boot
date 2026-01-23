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

package org.springframework.boot.jetty;

import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionConfig.Builder;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import org.springframework.boot.web.server.Compression;

/**
 * Jetty {@code HandlerWrapper} static factory.
 *
 * @author Brian Clozel
 */
final class JettyHandlerWrappers {

	private JettyHandlerWrappers() {
	}

	static Handler.Wrapper createGzipHandlerWrapper(Compression compression) {
		CompressionHandler compressionHandler = new CompressionHandler();
		GzipCompression gzip = new GzipCompression();
		gzip.setMinCompressSize((int) compression.getMinResponseSize().toBytes());
		compressionHandler.putCompression(gzip);
		Builder configBuilder = CompressionConfig.builder();
		for (String mimeType : compression.getAllMimeTypes()) {
			configBuilder.compressIncludeMimeType(mimeType);
		}
		for (HttpMethod httpMethod : HttpMethod.values()) {
			configBuilder.compressIncludeMethod(httpMethod.name());
		}
		compressionHandler.putConfiguration(PathSpec.from("/"), configBuilder.build());
		return compressionHandler;
	}

	static Handler.Wrapper createServerHeaderHandlerWrapper(String header) {
		return new ServerHeaderHandler(header);
	}

	/**
	 * {@link Handler.Wrapper} to add a custom {@code server} header.
	 */
	private static class ServerHeaderHandler extends Handler.Wrapper {

		private static final String SERVER_HEADER = "server";

		private final String value;

		ServerHeaderHandler(String value) {
			this.value = value;
		}

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
