/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.lang.reflect.Field;

import javax.annotation.PostConstruct;

import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Replaces the Objenesis instance in Spring HATEOAS's {@code DummyInvocationUtils} with
 * one that does not perform any caching. The cache is problematic as it's keyed on class
 * name which leads to {@code ClassCastExceptions} as the class loader changes across
 * restarts.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
class HateoasObjenesisCacheDisabler {

	private static boolean cacheDisabled;

	@PostConstruct
	void disableCaching() {
		if (!cacheDisabled) {
			cacheDisabled = true;
			doDisableCaching();
		}
	}

	private void doDisableCaching() {
		try {
			Class<?> type = ClassUtils.forName(
					"org.springframework.hateoas.core.DummyInvocationUtils",
					getClass().getClassLoader());
			Field objenesis = ReflectionUtils.findField(type, "OBJENESIS");
			if (objenesis != null) {
				ReflectionUtils.makeAccessible(objenesis);
				ReflectionUtils.setField(objenesis, null, new ObjenesisStd(false));
			}
		}
		catch (Exception ex) {
			// Assume that Spring HATEOAS is not on the classpath and continue
		}
	}

}
