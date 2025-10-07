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

package org.springframework.boot.test.web.reactive.client;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * A customizer that can be implemented by beans wishing to customize the
 * {@link WebTestClient.Builder} to fine-tune its auto-configuration before a
 * {@link WebTestClient} is created. Implementations can be registered in the
 * {@link ApplicationContext} or {@code spring.factories}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@FunctionalInterface
public interface WebTestClientBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder
	 */
	void customize(WebTestClient.Builder builder);

}
