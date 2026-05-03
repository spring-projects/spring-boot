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

package org.springframework.boot.resttestclient.autoconfigure;

import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.Builder;

/**
 * A customizer that can be implemented by beans wishing to customize the {@link Builder
 * RestTestClient.Builder} to fine-tune its auto-configuration before a
 * {@link RestTestClient} is created.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface RestTestClientBuilderCustomizer {

	/**
	 * Customize the given {@link Builder RestTestClient.Builder}.
	 * @param builder the builder
	 */
	void customize(RestTestClient.Builder<?> builder);

}
