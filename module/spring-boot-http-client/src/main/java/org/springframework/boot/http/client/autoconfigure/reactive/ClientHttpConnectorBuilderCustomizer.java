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

package org.springframework.boot.http.client.autoconfigure.reactive;

import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;

/**
 * Customizer that can be used to modify the auto-configured
 * {@link ClientHttpConnectorBuilder} when its type matches.
 *
 * @param <B> the builder type
 * @author Phillip Webb
 * @since 3.5.0
 */
public interface ClientHttpConnectorBuilderCustomizer<B extends ClientHttpConnectorBuilder<?>> {

	/**
	 * Customize the given builder.
	 * @param builder the builder to customize
	 * @return the customized builder
	 */
	B customize(B builder);

}
