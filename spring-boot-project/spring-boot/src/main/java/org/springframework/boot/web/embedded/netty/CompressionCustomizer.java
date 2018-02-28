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

package org.springframework.boot.web.embedded.netty;

import java.util.function.BiPredicate;

import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.ipc.netty.http.server.HttpServerOptions;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import org.springframework.boot.web.server.Compression;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Configure the HTTP compression on an Reactor Netty request/response handler.
 *
 * @author Stephane Maldini
 */
final class CompressionCustomizer implements NettyServerCustomizer {

	private final Compression compression;

	CompressionCustomizer(Compression compression) {
		this.compression = compression;
	}

	@Override
	public void customize(HttpServerOptions.Builder builder) {
		if (compression.getMinResponseSize() >= 0) {
			builder.compression(compression.getMinResponseSize());
		}
		
		BiPredicate<HttpServerRequest, HttpServerResponse> compressPredicate = null;

		if (compression.getMimeTypes() != null &&
				compression.getMimeTypes().length > 0) {
			compressPredicate = new CompressibleMimeTypePredicate(compression.getMimeTypes());
		}

		if (compression.getExcludedUserAgents() != null &&
				compression.getExcludedUserAgents().length > 0 ) {
			BiPredicate<HttpServerRequest, HttpServerResponse> agentCompressPredicate =
					new CompressibleAgentPredicate(compression.getExcludedUserAgents());

			compressPredicate = compressPredicate == null ?
					agentCompressPredicate :
					compressPredicate.and(agentCompressPredicate);
		}

		if (compressPredicate != null) {
			builder.compression(compressPredicate);
		}
	}

	private static class CompressibleAgentPredicate
			implements BiPredicate<HttpServerRequest, HttpServerResponse> {

		private final String[] excludedAgents;

		CompressibleAgentPredicate(String[] excludedAgents) {
			this.excludedAgents = new String[excludedAgents.length];
			System.arraycopy(excludedAgents, 0, this.excludedAgents, 0, excludedAgents.length);
		}

		@Override
		public boolean test(HttpServerRequest request, HttpServerResponse response) {
			for(String excludedAgent : excludedAgents) {
				if (request.requestHeaders()
				           .contains(HttpHeaderNames.USER_AGENT, excludedAgent, true)) {
					return false;
				}
			}
			return true;
		}
	}

	private static class CompressibleMimeTypePredicate
			implements BiPredicate<HttpServerRequest, HttpServerResponse> {

		private final MimeType[] mimeTypes;

		CompressibleMimeTypePredicate(String[] mimeTypes) {
			this.mimeTypes = new MimeType[mimeTypes.length];
			for (int i = 0; i < mimeTypes.length; i++) {
				this.mimeTypes[i] = MimeTypeUtils.parseMimeType(mimeTypes[i]);
			}
		}

		@Override
		public boolean test(HttpServerRequest request, HttpServerResponse response) {
			String contentType = response.responseHeaders()
			                             .get(HttpHeaderNames.CONTENT_TYPE);
			if (contentType != null) {
				for (MimeType mimeType : this.mimeTypes) {
					if (mimeType.isCompatibleWith(MimeTypeUtils.parseMimeType(contentType))) {
						return true;
					}
				}
			}
			return false;
		}

	}
}
