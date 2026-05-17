/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.event;

import org.jspecify.annotations.Nullable;

/**
 * Interface to be implemented by events that publish a bound network port.
 *
 * @author Somil Jain
 * @since 4.0.0
 */
public interface PortBound {

	/**
	 * Return the port that the server bound to.
	 * @return the bound port
	 */
	int getPort();

	/**
	 * Return the namespace of the server, if any.
	 * @return the server namespace or {@code null}
	 */
	@Nullable String getNamespace();

}
