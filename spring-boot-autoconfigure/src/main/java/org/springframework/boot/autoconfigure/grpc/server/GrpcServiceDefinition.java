/*
 * Copyright 2016-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.grpc.server;

import io.grpc.ServerServiceDefinition;

/**
 * A data class to capture the discovered gRPC services.
 * @author Ray Tsang
 */
public class GrpcServiceDefinition {
	private final String beanName;
	private final Class<?> beanClazz;
	private final ServerServiceDefinition definition;

	public GrpcServiceDefinition(String beanName, Class<?> beanClazz,
			ServerServiceDefinition definition) {
		super();
		this.beanName = beanName;
		this.beanClazz = beanClazz;
		this.definition = definition;
	}

	public String getBeanName() {
		return this.beanName;
	}

	public Class<?> getBeanClazz() {
		return this.beanClazz;
	}

	public ServerServiceDefinition getDefinition() {
		return this.definition;
	}
}
