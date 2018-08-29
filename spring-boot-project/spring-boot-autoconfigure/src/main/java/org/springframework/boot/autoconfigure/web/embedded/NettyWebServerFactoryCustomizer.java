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

package org.springframework.boot.autoconfigure.web.embedded;

import java.util.function.Function;
import java.util.function.Predicate;

import reactor.netty.http.server.HttpRequestDecoderSpec;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Customization for Netty-specific features.
 *
 * @author Brian Clozel
 * @author Samuel Ko
 * @since 2.1.0
 */
public class NettyWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory>, Ordered {

	private static final Predicate<Integer> isStrictlyPositive = (n) -> n > 0;

	private final Environment environment;

	private final ServerProperties serverProperties;

	public NettyWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(NettyReactiveWebServerFactory factory) {
		factory.addServerCustomizers((httpServer) -> httpServer
				.httpRequestDecoder(this.httpRequestDecoderSpecMapper));

		factory.setUseForwardHeaders(
				getOrDeduceUseForwardHeaders(this.serverProperties, this.environment));
	}

	Function<HttpRequestDecoderSpec, HttpRequestDecoderSpec> httpRequestDecoderSpecMapper = (
			httpRequestDecoderSpec) -> httpRequestDecoderSpec
					.maxChunkSize(determineMaxChunkSize())
					.maxHeaderSize(determineMaxHeaderSize())
					.maxInitialLineLength(determineMaxInitialLineLength())
					.validateHeaders(determineValidateHeaders())
					.initialBufferSize(determineInitialBufferSize());

	private boolean determineValidateHeaders() {
		return this.serverProperties.getNetty().getValidateHeaders();
	}

	private boolean isStrictlyPositive(int n) {
		return isStrictlyPositive.test(n);
	}

	private int determineMaxHeaderSize() {
		if (isStrictlyPositive(this.serverProperties.getMaxHttpHeaderSize())) {
			return this.serverProperties.getMaxHttpHeaderSize();
		}
		else if (isStrictlyPositive(
				this.serverProperties.getNetty().getMaxHeaderSize())) {
			return this.serverProperties.getNetty().getMaxHeaderSize();
		}

		return HttpRequestDecoderSpec.DEFAULT_MAX_HEADER_SIZE;
	}

	private int determineInitialBufferSize() {
		return isStrictlyPositive(this.serverProperties.getNetty().getInitialBufferSize())
				? this.serverProperties.getNetty().getInitialBufferSize()
				: HttpRequestDecoderSpec.DEFAULT_INITIAL_BUFFER_SIZE;
	}

	private int determineMaxInitialLineLength() {
		return isStrictlyPositive(
				this.serverProperties.getNetty().getMaxInitialLineLength())
						? this.serverProperties.getNetty().getMaxInitialLineLength()
						: HttpRequestDecoderSpec.DEFAULT_MAX_INITIAL_LINE_LENGTH;
	}

	private int determineMaxChunkSize() {
		return isStrictlyPositive(this.serverProperties.getNetty().getMaxChunkSize())
				? this.serverProperties.getNetty().getMaxChunkSize()
				: HttpRequestDecoderSpec.DEFAULT_MAX_CHUNK_SIZE;
	}

	private boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

}
