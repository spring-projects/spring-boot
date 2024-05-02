/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.redis;

import io.lettuce.core.ClientOptions;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ClientOptions} through a {@link ClientOptions.Builder} whilst retaining default
 * auto-configuration.
 *
 * @author Soohyun Lim
 */
@FunctionalInterface
public interface LettuceClientOptionsBuilderCustomizer {

	/**
	 * Customize the {@link ClientOptions.Builder}.
	 * @param clientOptionsBuilder the builder to customize
	 */
	void customize(ClientOptions.Builder clientOptionsBuilder);

}
