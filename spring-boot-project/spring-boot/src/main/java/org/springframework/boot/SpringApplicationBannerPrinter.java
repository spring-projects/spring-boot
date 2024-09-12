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

package org.springframework.boot;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.env.Environment;

/**
 * Interface class used to print the application banner.
 *
 * @author Phillip Webb
 * @author Junhyung Park
 * @since 3.4.0
 */
public interface SpringApplicationBannerPrinter {

	String BANNER_LOCATION_PROPERTY = "spring.banner.location";

	String DEFAULT_BANNER_LOCATION = "banner.txt";

	Banner print(Environment environment, Class<?> sourceClass, Banner.Mode mode, Banner banner);

	class SpringApplicationBannerPrinterRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.resources().registerPattern(DEFAULT_BANNER_LOCATION);
		}

	}

}
