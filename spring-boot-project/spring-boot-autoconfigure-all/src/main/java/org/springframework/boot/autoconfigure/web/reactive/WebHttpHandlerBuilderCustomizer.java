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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Callback interface used to customize a {@link WebHttpHandlerBuilder}.
 *
 * @author Lasse Wulff
 * @since 3.3.0
 */
@FunctionalInterface
public interface WebHttpHandlerBuilderCustomizer {

	/**
	 * Callback to customize a {@link WebHttpHandlerBuilder} instance.
	 * @param webHttpHandlerBuilder the {@link WebHttpHandlerBuilder} to customize
	 */
	void customize(WebHttpHandlerBuilder webHttpHandlerBuilder);

}
