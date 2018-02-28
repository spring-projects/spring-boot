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

import java.util.Arrays;
import java.util.function.BiPredicate;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.ipc.netty.http.server.HttpServerOptions;
import reactor.ipc.netty.http.server.HttpServerRequest;
import reactor.ipc.netty.http.server.HttpServerResponse;

import org.springframework.boot.web.server.Compression;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configure the HTTP compression on an Reactor Netty request/response handler.
 *
 * @author Stephane Maldini
 * @author Phillip Webb
 */
final class CompressionCustomizer implements NettyServerCustomizer {

	private static final CompressionPredicate ALWAYS_COMPRESS = (request,
			response) -> true;

	private final Compression compression;

	CompressionCustomizer(Compression compression) {
		this.compression = compression;
	}

	@Override
	public void customize(HttpServerOptions.Builder builder) {
		if (this.compression.getMinResponseSize() >= 0) {
			builder.compression(this.compression.getMinResponseSize());
		}
		CompressionPredicate mimeTypes = getMimeTypesPredicate(
				this.compression.getMimeTypes());
		CompressionPredicate excludedUserAgents = getExcludedUserAgentsPredicate(
				this.compression.getExcludedUserAgents());
		builder.compression(mimeTypes.and(excludedUserAgents));
	}

	private CompressionPredicate getMimeTypesPredicate(String[] mimeTypes) {
		if (ObjectUtils.isEmpty(mimeTypes)) {
			return ALWAYS_COMPRESS;
		}
		return (request, response) -> {
			String contentType = response.responseHeaders()
					.get(HttpHeaderNames.CONTENT_TYPE);
			if (StringUtils.isEmpty(contentType)) {
				return false;
			}
			MimeType contentMimeType = MimeTypeUtils.parseMimeType(contentType);
			return Arrays.stream(mimeTypes).map(MimeTypeUtils::parseMimeType)
					.anyMatch((candidate) -> candidate.isCompatibleWith(contentMimeType));
		};
	}

	private CompressionPredicate getExcludedUserAgentsPredicate(
			String[] excludedUserAgents) {
		if (ObjectUtils.isEmpty(excludedUserAgents)) {
			return ALWAYS_COMPRESS;
		}
		return (request, response) -> {
			HttpHeaders headers = request.requestHeaders();
			return Arrays.stream(excludedUserAgents).noneMatch((candidate) -> headers
					.contains(HttpHeaderNames.USER_AGENT, candidate, true));
		};
	}

	private interface CompressionPredicate
			extends BiPredicate<HttpServerRequest, HttpServerResponse> {

	}

}
