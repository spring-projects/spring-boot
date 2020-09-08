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

package org.springframework.boot.context.properties;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.Conventions;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

	static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

	static final String DEDUCE_BIND_CONSTRUCTOR_ATTRIUBTE = Conventions
			.getQualifiedAttributeName(ConfigurationPropertiesBindConstructorProvider.class, "deduceBindConstructor");

	@Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		Boolean deduceBindConstructor = (Boolean) bindable.getAttribute(DEDUCE_BIND_CONSTRUCTOR_ATTRIUBTE);
		return getBindConstructor(bindable.getType().resolve(), Boolean.TRUE.equals(deduceBindConstructor),
				isNestedConstructorBinding);
	}

	Constructor<?> getBindConstructor(Class<?> type, boolean deduceBindConstructor,
			boolean isNestedConstructorBinding) {
		if (type == null) {
			return null;
		}
		Constructor<?> constructor = findConstructorBindingAnnotatedConstructor(type);
		if (constructor != null) {
			return constructor;
		}
		boolean isConstructorBindingAnnotatedType = isConstructorBindingAnnotatedType(type);
		if (deduceBindConstructor || isNestedConstructorBinding || isConstructorBindingAnnotatedType) {
			constructor = deduceBindConstructor(type);
		}
		if (deduceBindConstructor && isConstructorBindingAnnotatedType && !isNestedConstructorBinding) {
			Assert.state(constructor != null,
					() -> "Unable to deduce constructor for @ConstructorBinding class " + type.getName());
			Assert.state(constructor.getParameterCount() > 0,
					() -> "Deduced no-args constructor for @ConstructorBinding class " + type.getName());
		}
		return constructor;
	}

	private Constructor<?> findConstructorBindingAnnotatedConstructor(Class<?> type) {
		if (isKotlinType(type)) {
			Constructor<?> constructor = BeanUtils.findPrimaryConstructor(type);
			if (constructor != null) {
				return findAnnotatedConstructor(type, constructor);
			}
		}
		return findAnnotatedConstructor(type, type.getDeclaredConstructors());
	}

	private Constructor<?> findAnnotatedConstructor(Class<?> type, Constructor<?>... candidates) {
		Constructor<?> constructor = null;
		for (Constructor<?> candidate : candidates) {
			if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
				Assert.state(candidate.getParameterCount() > 0,
						() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
				Assert.state(constructor == null,
						() -> type.getName() + " has more than one @ConstructorBinding constructor");
				constructor = candidate;
			}
		}
		return constructor;
	}

	private boolean isConstructorBindingAnnotatedType(Class<?> type) {
		return MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
				.isPresent(ConstructorBinding.class);
	}

	private Constructor<?> deduceBindConstructor(Class<?> type) {
		if (isKotlinType(type)) {
			return deducedKotlinBindConstructor(type);
		}
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		if (constructors.length == 1) {
			return constructors[0];
		}
		return null;
	}

	private Constructor<?> deducedKotlinBindConstructor(Class<?> type) {
		Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
		if (primaryConstructor != null) {
			return primaryConstructor;
		}
		return null;
	}

	private boolean isKotlinType(Class<?> type) {
		return KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type);
	}

}
