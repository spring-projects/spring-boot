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

package org.springframework.boot.http.converter.autoconfigure;

import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;

/**
 * Callback interface that can be used to customize a {@link HttpMessageConverters} for
 * server usage.
 *
 * @author Brian Clozel
 * @since 4.0
 */
@FunctionalInterface
public interface ServerHttpMessageConvertersCustomizer {

	/**
	 * Callback to customize a {@link ServerBuilder} instance.
	 * @param builder the builder to customize
	 */
	void customize(ServerBuilder builder);

}
