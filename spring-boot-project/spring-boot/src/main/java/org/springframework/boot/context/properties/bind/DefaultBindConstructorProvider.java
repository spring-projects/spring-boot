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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

	/**
     * Returns the bind constructor for the given bindable object.
     * 
     * @param bindable                  the bindable object
     * @param isNestedConstructorBinding true if the bindable object is a nested constructor binding, false otherwise
     * @return                          the bind constructor, or null if not found
     */
    @Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		Constructors constructors = Constructors.getConstructors(bindable.getType().resolve(),
				isNestedConstructorBinding);
		if (constructors.getBind() != null && constructors.isDeducedBindConstructor()
				&& !constructors.isImmutableType()) {
			if (bindable.getValue() != null && bindable.getValue().get() != null) {
				return null;
			}
		}
		return constructors.getBind();
	}

	/**
     * Returns the bind constructor for the given type.
     * 
     * @param type the type for which to retrieve the bind constructor
     * @param isNestedConstructorBinding true if the constructor is a nested constructor binding, false otherwise
     * @return the bind constructor for the given type
     */
    @Override
	public Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
		Constructors constructors = Constructors.getConstructors(type, isNestedConstructorBinding);
		return constructors.getBind();
	}

	/**
	 * Data holder for autowired and bind constructors.
	 */
	static final class Constructors {

		private static final Constructors NONE = new Constructors(false, null, false, false);

		private final boolean hasAutowired;

		private final Constructor<?> bind;

		private final boolean deducedBindConstructor;

		private final boolean immutableType;

		/**
         * Constructs a new instance of the Constructors class.
         * 
         * @param hasAutowired              a boolean indicating whether the class has autowired dependencies
         * @param bind                      the constructor to bind to the class
         * @param deducedBindConstructor    a boolean indicating whether the bind constructor was deduced
         * @param immutableType             a boolean indicating whether the class is of immutable type
         */
        private Constructors(boolean hasAutowired, Constructor<?> bind, boolean deducedBindConstructor,
				boolean immutableType) {
			this.hasAutowired = hasAutowired;
			this.bind = bind;
			this.deducedBindConstructor = deducedBindConstructor;
			this.immutableType = immutableType;
		}

		/**
         * Returns a boolean value indicating whether the object has been autowired.
         *
         * @return true if the object has been autowired, false otherwise
         */
        boolean hasAutowired() {
			return this.hasAutowired;
		}

		/**
         * Returns the bind constructor.
         *
         * @return the bind constructor
         */
        Constructor<?> getBind() {
			return this.bind;
		}

		/**
         * Returns a boolean value indicating whether the bind constructor is deduced.
         *
         * @return {@code true} if the bind constructor is deduced, {@code false} otherwise.
         */
        boolean isDeducedBindConstructor() {
			return this.deducedBindConstructor;
		}

		/**
         * Returns a boolean value indicating whether the type is immutable or not.
         *
         * @return true if the type is immutable, false otherwise.
         */
        boolean isImmutableType() {
			return this.immutableType;
		}

		/**
         * Retrieves the constructors for a given class, considering the specified parameters.
         * 
         * @param type the class for which to retrieve the constructors
         * @param isNestedConstructorBinding flag indicating whether the constructor binding is nested
         * @return the constructors for the given class
         */
        static Constructors getConstructors(Class<?> type, boolean isNestedConstructorBinding) {
			if (type == null) {
				return NONE;
			}
			boolean hasAutowiredConstructor = isAutowiredPresent(type);
			Constructor<?>[] candidates = getCandidateConstructors(type);
			MergedAnnotations[] candidateAnnotations = getAnnotations(candidates);
			boolean deducedBindConstructor = false;
			boolean immutableType = type.isRecord();
			Constructor<?> bind = getConstructorBindingAnnotated(type, candidates, candidateAnnotations);
			if (bind == null && !hasAutowiredConstructor) {
				bind = deduceBindConstructor(type, candidates);
				deducedBindConstructor = bind != null;
			}
			if (bind == null && !hasAutowiredConstructor && isKotlinType(type)) {
				bind = deduceKotlinBindConstructor(type);
				deducedBindConstructor = bind != null;
			}
			if (bind != null || isNestedConstructorBinding) {
				Assert.state(!hasAutowiredConstructor,
						() -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
			}
			return new Constructors(hasAutowiredConstructor, bind, deducedBindConstructor, immutableType);
		}

		/**
         * Checks if the given class or any of its superclasses have the {@code Autowired} annotation present on any of its constructors.
         * 
         * @param type the class to check
         * @return {@code true} if the {@code Autowired} annotation is present on any constructor, {@code false} otherwise
         */
        private static boolean isAutowiredPresent(Class<?> type) {
			if (Stream.of(type.getDeclaredConstructors())
				.map(MergedAnnotations::from)
				.anyMatch((annotations) -> annotations.isPresent(Autowired.class))) {
				return true;
			}
			Class<?> userClass = ClassUtils.getUserClass(type);
			return (userClass != type) && isAutowiredPresent(userClass);
		}

		/**
         * Returns an array of candidate constructors for the given type.
         * 
         * @param type the class for which to retrieve the constructors
         * @return an array of candidate constructors
         */
        private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
			if (isInnerClass(type)) {
				return new Constructor<?>[0];
			}
			return Arrays.stream(type.getDeclaredConstructors())
				.filter(Constructors::isNonSynthetic)
				.toArray(Constructor[]::new);
		}

		/**
         * Checks if the given class is an inner class.
         * 
         * @param type the class to check
         * @return true if the class is an inner class, false otherwise
         */
        private static boolean isInnerClass(Class<?> type) {
			try {
				return type.getDeclaredField("this$0").isSynthetic();
			}
			catch (NoSuchFieldException ex) {
				return false;
			}
		}

		/**
         * Checks if a constructor is non-synthetic.
         * 
         * @param constructor the constructor to check
         * @return true if the constructor is non-synthetic, false otherwise
         */
        private static boolean isNonSynthetic(Constructor<?> constructor) {
			return !constructor.isSynthetic();
		}

		/**
         * Retrieves the merged annotations for an array of constructors.
         * 
         * @param candidates the array of constructors to retrieve annotations from
         * @return an array of merged annotations for each constructor
         */
        private static MergedAnnotations[] getAnnotations(Constructor<?>[] candidates) {
			MergedAnnotations[] candidateAnnotations = new MergedAnnotations[candidates.length];
			for (int i = 0; i < candidates.length; i++) {
				candidateAnnotations[i] = MergedAnnotations.from(candidates[i], SearchStrategy.SUPERCLASS);
			}
			return candidateAnnotations;
		}

		/**
         * Returns the constructor annotated with {@link ConstructorBinding} from the given array of candidate constructors.
         * 
         * @param type the class type
         * @param candidates the array of candidate constructors
         * @param mergedAnnotations the array of merged annotations for each candidate constructor
         * @return the constructor annotated with {@link ConstructorBinding}, or null if none found
         * @throws IllegalStateException if the class type declares {@link ConstructorBinding} on a no-args constructor
         * @throws IllegalStateException if the class type has more than one constructor annotated with {@link ConstructorBinding}
         */
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

		/**
         * Deduces the bind constructor for the given type from the provided candidates.
         * 
         * @param type       the type for which to deduce the bind constructor
         * @param candidates the array of constructor candidates
         * @return the deduced bind constructor, or null if none found
         */
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

		/**
         * Checks if the given type is a Kotlin type.
         * 
         * @param type the type to be checked
         * @return {@code true} if the type is a Kotlin type, {@code false} otherwise
         */
        private static boolean isKotlinType(Class<?> type) {
			return KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type);
		}

		/**
         * Deduces the Kotlin bind constructor for the given type.
         * 
         * @param type the class for which to deduce the bind constructor
         * @return the bind constructor if found, otherwise null
         */
        private static Constructor<?> deduceKotlinBindConstructor(Class<?> type) {
			Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
			if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
				return primaryConstructor;
			}
			return null;
		}

	}

}
