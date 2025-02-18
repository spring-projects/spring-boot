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

/**
 * Configuration property binding methods.
 *
 * @author Andy Wilkinson
 * @since 3.0.8
 */
public enum BindMethod {

	/**
	 * Java Bean using getter/setter binding.
	 */
	JAVA_BEAN,

	/**
	 * Value object using constructor binding.
	 */
	VALUE_OBJECT

}
