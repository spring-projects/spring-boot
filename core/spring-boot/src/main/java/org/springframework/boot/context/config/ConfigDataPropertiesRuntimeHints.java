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

package org.springframework.boot.context.config;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for {@link ConfigDataProperties}.
 *
 * @author Moritz Halbritter
 */
class ConfigDataPropertiesRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		BindableRuntimeHintsRegistrar.forTypes(ConfigDataProperties.class).registerHints(hints);
		Method method = ReflectionUtils.findMethod(ConfigDataLocation.class, "of", String.class);
		Assert.state(method != null, "'method' must not be null");
		hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
	}

}
