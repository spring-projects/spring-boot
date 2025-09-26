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

package org.springframework.boot.grpc.server.autoconfigure;

import io.grpc.netty.NettyServerBuilder;

import org.springframework.grpc.server.NettyGrpcServerFactory;

/**
 * Helper class used to map {@link GrpcServerProperties} to
 * {@link NettyGrpcServerFactory}.
 *
 * @author Chris Bono
 */
class NettyServerFactoryPropertyMapper extends DefaultServerFactoryPropertyMapper<NettyServerBuilder> {

	NettyServerFactoryPropertyMapper(GrpcServerProperties properties) {
		super(properties);
	}

	@Override
	void customizeServerBuilder(NettyServerBuilder nettyServerBuilder) {
		super.customizeServerBuilder(nettyServerBuilder);
	}

}
