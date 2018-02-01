/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import java.util.Map;

/**
 * Interface to perform an operation invocation.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface OperationInvoker {

	/**
	 * Invoke the underlying operation using the given {@code arguments}.
	 * @param arguments the arguments to pass to the operation
	 * @return the result of the operation, may be {@code null}
	 * @throws MissingParametersException if parameters are missing
	 */
	Object invoke(Map<String, Object> arguments) throws MissingParametersException;

}
