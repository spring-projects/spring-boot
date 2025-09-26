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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.grpc.ServerBuilder;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.grpc.server.ServerBuilderCustomizer;

/**
 * Invokes the available {@link ServerBuilderCustomizer} instances in the context for a
 * given {@link ServerBuilder}.
 *
 * @author Chris Bono
 */
class ServerBuilderCustomizers {

	private final List<ServerBuilderCustomizer<?>> customizers;

	ServerBuilderCustomizers(List<? extends ServerBuilderCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList();
	}

	/**
	 * Customize the specified {@link ServerBuilder}. Locates all
	 * {@link ServerBuilderCustomizer} beans able to handle the specified instance and
	 * invoke {@link ServerBuilderCustomizer#customize} on them.
	 * @param <T> the type of server builder
	 * @param serverBuilder the builder to customize
	 * @return the customized builder
	 */
	@SuppressWarnings("unchecked")
	<T extends ServerBuilder<?>> T customize(T serverBuilder) {
		LambdaSafe.callbacks(ServerBuilderCustomizer.class, this.customizers, serverBuilder)
			.withLogger(ServerBuilderCustomizers.class)
			.invoke((customizer) -> customizer.customize(serverBuilder));
		return serverBuilder;
	}

}
