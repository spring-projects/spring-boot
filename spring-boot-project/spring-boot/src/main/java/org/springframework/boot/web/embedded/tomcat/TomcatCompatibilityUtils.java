/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.lang.reflect.Method;
import java.util.Optional;

import org.apache.tomcat.util.compat.JreCompat;
import org.apache.tomcat.util.modeler.Registry;

import org.springframework.util.ReflectionUtils;

final class TomcatCompatibilityUtils {

	private TomcatCompatibilityUtils() {
	}

	static boolean isGraalAvailable() {
		Optional<Method> optionalMethod = Optional
				.ofNullable(ReflectionUtils.findMethod(JreCompat.class, "isGraalAvailable"));
		return optionalMethod.map((method) -> ReflectionUtils.invokeMethod(method, null)).map(Boolean.class::cast)
				.orElse(false);
	}

	static void disableRegistry() {
		Optional<Method> optionalMethod = Optional
				.ofNullable(ReflectionUtils.findMethod(Registry.class, "disableRegistry"));
		optionalMethod.map((method) -> ReflectionUtils.invokeMethod(method, null));
	}

}
