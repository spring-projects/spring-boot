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

package org.springframework.boot.test.autoconfigure.json;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.test.json.AbstractJsonMarshalTester;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Base class for {@link AbstractJsonMarshalTester} runtime hints.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@SuppressWarnings("rawtypes")
public abstract class JsonMarshalTesterRuntimeHints implements RuntimeHintsRegistrar {

	private final Class<? extends AbstractJsonMarshalTester> tester;

	protected JsonMarshalTesterRuntimeHints(Class<? extends AbstractJsonMarshalTester> tester) {
		this.tester = tester;
	}

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		ReflectionHints reflection = hints.reflection();
		reflection.registerType(this.tester, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		Method method = ReflectionUtils.findMethod(this.tester, "initialize", Class.class, ResolvableType.class);
		Assert.state(method != null, "'method' must not be null");
		reflection.registerMethod(method, ExecutableMode.INVOKE);
	}

}
