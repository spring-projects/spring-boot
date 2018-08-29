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

import java.util.function.Predicate;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
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
		customizeHttpRequestDecoderSpec(factory);

		factory.setUseForwardHeaders(
				getOrDeduceUseForwardHeaders(this.serverProperties, this.environment));
	}

	private void customizeHttpRequestDecoderSpec(NettyReactiveWebServerFactory factory) {
		final ServerProperties.Netty nettyProperties = this.serverProperties.getNetty();
		final PropertyMapper propertyMapper = PropertyMapper.get();

		propertyMapper.from(nettyProperties::getMaxInitialLineLength)
				.when(isStrictlyPositive)
				.to((maxInitialLineLength) -> customizeMaxInitialLineLength(factory,
						maxInitialLineLength));
		propertyMapper.from(determineMaxHeaderSize()).when(isStrictlyPositive)
				.to((maxHeaderSize) -> customizeMaxHeaderSize(factory, maxHeaderSize));
		propertyMapper.from(nettyProperties::getMaxChunkSize).when(isStrictlyPositive)
				.to((maxChunkSize) -> customizeMaxChunkSize(factory, maxChunkSize));
		propertyMapper.from(nettyProperties::getValidateHeaders).to(
				(validateHeaders) -> customizeValidateHeaders(factory, validateHeaders));
		propertyMapper.from(nettyProperties::getInitialBufferSize)
				.when(isStrictlyPositive);
	}

	private void customizeMaxChunkSize(NettyReactiveWebServerFactory factory,
			int maxChunkSize) {
		factory.addServerCustomizers((httpServer) -> httpServer
				.httpRequestDecoder((httpRequestDecoderSpec) -> httpRequestDecoderSpec
						.maxChunkSize(maxChunkSize)));
	}

	private void customizeValidateHeaders(NettyReactiveWebServerFactory factory,
			boolean validateHeaders) {
		factory.addServerCustomizers((httpServer) -> httpServer
				.httpRequestDecoder((httpRequestDecoderSpec) -> httpRequestDecoderSpec
						.validateHeaders(validateHeaders)));
	}

	private void customizeMaxHeaderSize(NettyReactiveWebServerFactory factory,
			int maxHeaderSize) {
		factory.addServerCustomizers((httpServer) -> httpServer
				.httpRequestDecoder((httpRequestDecoderSpec) -> httpRequestDecoderSpec
						.maxHeaderSize(maxHeaderSize)));
	}

	private void customizeMaxInitialLineLength(NettyReactiveWebServerFactory factory,
			int maxInitialLineLength) {
		factory.addServerCustomizers((httpServer) -> httpServer
				.httpRequestDecoder((httpRequestDecoderSpec) -> httpRequestDecoderSpec
						.maxInitialLineLength(maxInitialLineLength)));
	}

	private int determineMaxHeaderSize() {
		return (this.serverProperties.getMaxHttpHeaderSize() > 0)
				? this.serverProperties.getMaxHttpHeaderSize()
				: this.serverProperties.getNetty().getMaxHeaderSize();
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
