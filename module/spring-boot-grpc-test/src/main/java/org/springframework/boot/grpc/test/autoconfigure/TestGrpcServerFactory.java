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

package org.springframework.boot.grpc.test.autoconfigure;

import java.util.Collections;

import io.grpc.inprocess.InProcessServerBuilder;

import org.springframework.grpc.server.DefaultGrpcServerFactory;
import org.springframework.grpc.server.GrpcServerFactory;

/**
 * {@link GrpcServerFactory} for testing with in-process transport.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class TestGrpcServerFactory extends DefaultGrpcServerFactory<InProcessServerBuilder> {

	TestGrpcServerFactory(String address) {
		super(address, Collections.emptyList(), null, null, null);
	}

	@Override
	protected InProcessServerBuilder newServerBuilder() {
		return InProcessServerBuilder.forName(address());
	}

}
