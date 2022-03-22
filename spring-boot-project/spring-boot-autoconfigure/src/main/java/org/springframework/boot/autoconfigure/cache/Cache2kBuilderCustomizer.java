/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import org.cache2k.Cache2kBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the default
 * setup for caches added to the manager via addCaches and for dynamically created caches.
 *
 * @author Jens Wilke
 * @author Stephane Nicoll
 * @since 2.7.0
 */
public interface Cache2kBuilderCustomizer {

	/**
	 * Customize the default cache settings.
	 * @param builder the builder to customize
	 */
	void customize(Cache2kBuilder<?, ?> builder);

}
