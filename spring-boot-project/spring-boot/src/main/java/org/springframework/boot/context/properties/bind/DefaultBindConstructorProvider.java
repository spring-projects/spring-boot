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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

	@Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		return getBindConstructor(bindable.getType().resolve(), isNestedConstructorBinding);
	}

	@Override
	public Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
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
			MergedAnnotations[] candidateAnnotations = getAnnotations(candidates);
			boolean hasAutowiredConstructor = isAutowiredPresent(candidateAnnotations);
			Constructor<?> bind = getConstructorBindingAnnotated(type, candidates, candidateAnnotations);
			if (bind == null && !hasAutowiredConstructor) {
				bind = deduceBindConstructor(type, candidates);
			}
			if (bind == null && !hasAutowiredConstructor && isKotlinType(type)) {
				bind = deduceKotlinBindConstructor(type);
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

		private static MergedAnnotations[] getAnnotations(Constructor<?>[] candidates) {
			MergedAnnotations[] candidateAnnotations = new MergedAnnotations[candidates.length];
			for (int i = 0; i < candidates.length; i++) {
				candidateAnnotations[i] = MergedAnnotations.from(candidates[i]);
			}
			return candidateAnnotations;
		}

		private static boolean isAutowiredPresent(MergedAnnotations[] candidateAnnotations) {
			for (MergedAnnotations annotations : candidateAnnotations) {
				if (annotations.isPresent(Autowired.class)) {
					return true;
				}
			}
			return false;
		}

		private static Constructor<?> getConstructorBindingAnnotated(Class<?> type, Constructor<?>[] candidates,
				MergedAnnotations[] mergedAnnotations) {
			Constructor<?> result = null;
			for (int i = 0; i < candidates.length; i++) {
				if (mergedAnnotations[i].isPresent(ConstructorBinding.class)) {
					Assert.state(candidates[i].getParameterCount() > 0,
							() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
					Assert.state(result == null,
							() -> type.getName() + " has more than one @ConstructorBinding constructor");
					result = candidates[i];
				}
			}
			return result;

		}

		private static Constructor<?> deduceBindConstructor(Class<?> type, Constructor<?>[] candidates) {
			if (candidates.length == 1 && candidates[0].getParameterCount() > 0) {
				if (type.isMemberClass() && Modifier.isPrivate(candidates[0].getModifiers())) {
					return null;
				}
				return candidates[0];
			}
			Constructor<?> result = null;
			for (Constructor<?> candidate : candidates) {
				if (!Modifier.isPrivate(candidate.getModifiers())) {
					if (result != null) {
						return null;
					}
					result = candidate;
				}
			}
			return (result != null && result.getParameterCount() > 0) ? result : null;
		}

		private static boolean isKotlinType(Class<?> type) {
			return KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type);
		}

		private static Constructor<?> deduceKotlinBindConstructor(Class<?> type) {
			Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
			if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
				return primaryConstructor;
			}
			return null;
		}

	}

}
