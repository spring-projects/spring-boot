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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeanUtils;
import org.springframework.core.KotlinDetector;

/**
 * Default {@link BindConstructorProvider} implementation.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {

	@Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		Class<?> type = bindable.getType().resolve();
		if (bindable.getValue() != null || type == null) {
			return null;
		}
		if (KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type)) {
			return getDeducedKotlinConstructor(type);
		}
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
			return constructors[0];
		}
		return null;
	}

	private Constructor<?> getDeducedKotlinConstructor(Class<?> type) {
		Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
		if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
			return primaryConstructor;
		}
		return null;
	}

}
