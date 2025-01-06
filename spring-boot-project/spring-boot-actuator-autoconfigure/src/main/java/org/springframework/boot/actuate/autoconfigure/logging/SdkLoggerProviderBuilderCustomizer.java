/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.logging;

import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;

/**
 * Callback interface that can be used to customize the {@link SdkLoggerProviderBuilder}
 * that is used to create the auto-configured {@link SdkLoggerProvider}.
 *
 * @author Toshiaki Maki
 * @since 3.4.0
 */
@FunctionalInterface
public interface SdkLoggerProviderBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder to customize
	 */
	void customize(SdkLoggerProviderBuilder builder);

}
