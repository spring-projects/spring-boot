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

package org.springframework.boot.logging.log4j2;

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.PropertySource;

/**
 * Spring Boot {@link PropertySource} that disables Log4j2's shutdown hook.
 *
 * @author Andy Wilkinson
 * @since 2.5.2
 */
public class SpringBootPropertySource implements PropertySource {

	private static final String PREFIX = "log4j.";

	private final Map<String, String> properties = Collections
			.singletonMap(ShutdownCallbackRegistry.SHUTDOWN_HOOK_ENABLED, "false");

	@Override
	public void forEach(BiConsumer<String, String> action) {
		this.properties.forEach(action::accept);
	}

	@Override
	public CharSequence getNormalForm(Iterable<? extends CharSequence> tokens) {
		return PREFIX + Util.joinAsCamelCase(tokens);
	}

	@Override
	public int getPriority() {
		return -200;
	}

}
