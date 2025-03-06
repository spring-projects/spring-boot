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

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.ClientResources.Builder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ClientResources} through a {@link Builder} whilst retaining default
 * auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
public interface ClientResourcesBuilderCustomizer {

	/**
	 * Customize the {@link Builder}.
	 * @param clientResourcesBuilder the builder to customize
	 */
	void customize(Builder clientResourcesBuilder);

}
