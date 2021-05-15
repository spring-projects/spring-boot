/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link DriverConfigLoader} via a {@link DriverConfigLoaderBuilderCustomizer} whilst
 * retaining default auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public interface DriverConfigLoaderBuilderCustomizer {

	/**
	 * Customize the {@linkplain ProgrammaticDriverConfigLoaderBuilder DriverConfigLoader
	 * builder}.
	 * @param builder the builder to customize
	 */
	void customize(ProgrammaticDriverConfigLoaderBuilder builder);

}
