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

package org.springframework.boot.actuate.endpoint;

/**
 * An operation on an {@link ExposableEndpoint endpoint}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface Operation {

	/**
	 * Returns the {@link OperationType type} of the operation.
	 * @return the type
	 */
	OperationType getType();

	/**
	 * Invoke the underlying operation using the given {@code context}.
	 * @param context the context in to use when invoking the operation
	 * @return the result of the operation, may be {@code null}
	 */
	Object invoke(InvocationContext context);

}
