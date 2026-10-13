/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.netty;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.jspecify.annotations.Nullable;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import org.springframework.boot.web.server.Compression;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configure the HTTP compression on a Reactor Netty request/response handler.
 *
 * @author Stephane Maldini
 * @author Phillip Webb
 * @author Brian Clozel
 */
final class CompressionCustomizer implements NettyServerCustomizer {

	private static final CompressionPredicate ALWAYS_COMPRESS = (request, response) -> true;

	private final Compression compression;

	CompressionCustomizer(Compression compression) {
		this.compression = compression;
	}

	@Override
	public HttpServer apply(HttpServer server) {
		if (!this.compression.getMinResponseSize().isNegative()) {
			server = server.compress((int) this.compression.getMinResponseSize().toBytes());
		}
		CompressionPredicate mimeTypes = getMimeTypesPredicate(this.compression.getAllMimeTypes());
		CompressionPredicate excludedUserAgents = getExcludedUserAgentsPredicate(
				this.compression.getExcludedUserAgents());
		server = server.compress(mimeTypes.and(excludedUserAgents));
		return server;
	}

	private CompressionPredicate getMimeTypesPredicate(String[] mimeTypeValues) {
		if (ObjectUtils.isEmpty(mimeTypeValues)) {
			return ALWAYS_COMPRESS;
		}
		List<MimeType> mimeTypes = Arrays.stream(mimeTypeValues).map(MimeTypeUtils::parseMimeType).toList();
		return (request, response) -> {
			String contentType = response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE);
			if (!StringUtils.hasLength(contentType)) {
				return false;
			}
			try {
				MimeType contentMimeType = MimeTypeUtils.parseMimeType(contentType);
				return mimeTypes.stream().anyMatch((candidate) -> candidate.isCompatibleWith(contentMimeType));
			}
			catch (InvalidMimeTypeException ex) {
				return false;
			}
		};
	}

	private CompressionPredicate getExcludedUserAgentsPredicate(String @Nullable [] excludedUserAgents) {
		if (ObjectUtils.isEmpty(excludedUserAgents)) {
			return ALWAYS_COMPRESS;
		}
		return (request, response) -> {
			HttpHeaders headers = request.requestHeaders();
			return Arrays.stream(excludedUserAgents)
				.noneMatch((candidate) -> headers.contains(HttpHeaderNames.USER_AGENT, candidate, true));
		};
	}

	private interface CompressionPredicate extends BiPredicate<HttpServerRequest, HttpServerResponse> {

	}

}
