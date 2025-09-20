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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.grpc.ManagedChannelBuilder;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

/**
 * Invokes the available {@link GrpcChannelBuilderCustomizer} instances for a given
 * {@link ManagedChannelBuilder}.
 *
 * @author Chris Bono
 * @since 4.0.0
 */
public class ChannelBuilderCustomizers {

	private final List<GrpcChannelBuilderCustomizer<?>> customizers;

	ChannelBuilderCustomizers(List<? extends GrpcChannelBuilderCustomizer<?>> customizers) {
		this.customizers = (customizers != null) ? new ArrayList<>(customizers) : Collections.emptyList();
	}

	/**
	 * Customize the specified {@link ManagedChannelBuilder}. Locates all
	 * {@link GrpcChannelBuilderCustomizer} beans able to handle the specified instance
	 * and invoke {@link GrpcChannelBuilderCustomizer#customize} on them.
	 * @param <T> the type of channel builder
	 * @param authority the target authority of the channel
	 * @param channelBuilder the builder to customize
	 * @return the customized builder
	 */
	@SuppressWarnings("unchecked")
	<T extends ManagedChannelBuilder<?>> T customize(String authority, T channelBuilder) {
		LambdaSafe.callbacks(GrpcChannelBuilderCustomizer.class, this.customizers, channelBuilder)
			.withLogger(ChannelBuilderCustomizers.class)
			.invoke((customizer) -> customizer.customize(authority, channelBuilder));
		return channelBuilder;
	}

}
