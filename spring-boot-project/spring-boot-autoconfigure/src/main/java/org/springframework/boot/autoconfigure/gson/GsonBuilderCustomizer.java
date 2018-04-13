/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link Gson} via {@link GsonBuilder} retaining its default auto-configuration.
 *
 * @author Ivan Golovko
 * @since 2.0.0
 */
@FunctionalInterface
public interface GsonBuilderCustomizer {

	/**
	 * Customize the GsonBuilder.
	 * @param gsonBuilder the GsonBuilder to customize
	 */
	void customize(GsonBuilder gsonBuilder);
}
