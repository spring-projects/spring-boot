/*
 * Copyright 2012-2017 the original author or authors.
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

/**
 * Binder that can be used by {@link BeanBinder} implementations to recursively bind bean
 * properties.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
interface BeanPropertyBinder {

	/**
	 * Bind the given property.
	 * @param propertyName the property name (in lowercase dashed form, e.g.
	 * {@code first-name})
	 * @param target the target bindable
	 * @return the bound value or {@code null}
	 */
	Object bindProperty(String propertyName, Bindable<?> target);

}
