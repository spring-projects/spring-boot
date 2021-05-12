/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import java.time.Duration;

import io.netty.channel.ChannelOption;

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
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
public class NettyWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public NettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(NettyReactiveWebServerFactory factory) {
		factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		ServerProperties.Netty nettyProperties = this.serverProperties.getNetty();
		propertyMapper.from(nettyProperties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeConnectionTimeout(factory, connectionTimeout));
		customizeRequestDecoder(factory, propertyMapper);
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	private void customizeConnectionTimeout(NettyReactiveWebServerFactory factory, Duration connectionTimeout) {
		factory.addServerCustomizers((httpServer) -> httpServer.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
				(int) connectionTimeout.toMillis()));
	}

	private void customizeRequestDecoder(NettyReactiveWebServerFactory factory, PropertyMapper propertyMapper) {
		factory.addServerCustomizers((httpServer) -> httpServer.httpRequestDecoder((httpRequestDecoderSpec) -> {
			propertyMapper.from(this.serverProperties.getMaxHttpHeaderSize()).whenNonNull()
					.to((maxHttpRequestHeader) -> httpRequestDecoderSpec
							.maxHeaderSize((int) maxHttpRequestHeader.toBytes()));
			ServerProperties.Netty nettyProperties = this.serverProperties.getNetty();
			propertyMapper.from(nettyProperties.getMaxChunkSize()).whenNonNull()
					.to((maxChunkSize) -> httpRequestDecoderSpec.maxChunkSize((int) maxChunkSize.toBytes()));
			propertyMapper.from(nettyProperties.getMaxInitialLineLength()).whenNonNull()
					.to((maxInitialLineLength) -> httpRequestDecoderSpec
							.maxInitialLineLength((int) maxInitialLineLength.toBytes()));
			propertyMapper.from(nettyProperties.getH2cMaxContentLength()).whenNonNull()
					.to((h2cMaxContentLength) -> httpRequestDecoderSpec
							.h2cMaxContentLength((int) h2cMaxContentLength.toBytes()));
			propertyMapper.from(nettyProperties.getInitialBufferSize()).whenNonNull().to(
					(initialBufferSize) -> httpRequestDecoderSpec.initialBufferSize((int) initialBufferSize.toBytes()));
			propertyMapper.from(nettyProperties.isValidateHeaders()).whenNonNull()
					.to(httpRequestDecoderSpec::validateHeaders);
			return httpRequestDecoderSpec;
		}));
	}

}
