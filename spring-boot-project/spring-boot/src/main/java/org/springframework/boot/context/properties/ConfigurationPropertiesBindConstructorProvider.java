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

package org.springframework.boot.context.properties;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 3.0.0
 */
public class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

	/**
	 * A shared singleton {@link ConfigurationPropertiesBindConstructorProvider} instance.
	 */
	public static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

	@Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		return getBindConstructor(bindable.getType().resolve(), isNestedConstructorBinding);
	}

	Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
		if (type == null) {
			return null;
		}
		Constructors constructors = Constructors.getConstructors(type);
		if (constructors.getBind() != null || isNestedConstructorBinding) {
			Assert.state(!constructors.hasAutowired(),
					() -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
		}
		return constructors.getBind();
	}

	/**
	 * Data holder for autowired and bind constructors.
	 */
	static final class Constructors {

		private final boolean hasAutowired;

		private final Constructor<?> bind;

		private Constructors(boolean hasAutowired, Constructor<?> bind) {
			this.hasAutowired = hasAutowired;
			this.bind = bind;
		}

		boolean hasAutowired() {
			return this.hasAutowired;
		}

		Constructor<?> getBind() {
			return this.bind;
		}

		static Constructors getConstructors(Class<?> type) {
			Constructor<?>[] candidates = getCandidateConstructors(type);
			Constructor<?> deducedBind = deduceBindConstructor(candidates);
			if (deducedBind != null) {
				return new Constructors(false, deducedBind);
			}
			boolean hasAutowiredConstructor = false;
			Constructor<?> bind = null;
			for (Constructor<?> candidate : candidates) {
				if (isAutowired(candidate)) {
					hasAutowiredConstructor = true;
					continue;
				}
				bind = findAnnotatedConstructor(type, bind, candidate);
			}
			return new Constructors(hasAutowiredConstructor, bind);
		}

		private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
			if (isInnerClass(type)) {
				return new Constructor<?>[0];
			}
			return Arrays.stream(type.getDeclaredConstructors())
					.filter((constructor) -> isNonSynthetic(constructor, type)).toArray(Constructor[]::new);
		}

		private static boolean isInnerClass(Class<?> type) {
			try {
				return type.getDeclaredField("this$0").isSynthetic();
			}
			catch (NoSuchFieldException ex) {
				return false;
			}
		}

		private static boolean isNonSynthetic(Constructor<?> constructor, Class<?> type) {
			return !constructor.isSynthetic();
		}

		private static Constructor<?> deduceBindConstructor(Constructor<?>[] constructors) {
			if (constructors.length == 1 && constructors[0].getParameterCount() > 0 && !isAutowired(constructors[0])) {
				return constructors[0];
			}
			return null;
		}

		private static boolean isAutowired(Constructor<?> candidate) {
			return MergedAnnotations.from(candidate).isPresent(Autowired.class);
		}

		private static Constructor<?> findAnnotatedConstructor(Class<?> type, Constructor<?> constructor,
				Constructor<?> candidate) {
			if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
				Assert.state(candidate.getParameterCount() > 0,
						() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
				Assert.state(constructor == null,
						() -> type.getName() + " has more than one @ConstructorBinding constructor");
				constructor = candidate;
			}
			return constructor;
		}

	}

}
