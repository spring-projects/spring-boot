/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

/**
 * Resolver for an argument of an {@link Operation}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public interface OperationArgumentResolver {

	/**
	 * Resolves an argument of the given {@code type}.
	 * @param <T> required type of the argument
	 * @param type argument type
	 * @return an argument of the required type, or {@code null}
	 */
	<T> T resolve(Class<T> type);

	/**
	 * Return whether an argument of the given {@code type} can be resolved.
	 * @param type argument type
	 * @return {@code true} if an argument of the required type can be resolved, otherwise
	 * {@code false}
	 */
	boolean canResolve(Class<?> type);

}
