/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.readiness;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.core.env.Environment;

/**
 * Strategy used to check if a {@link RunningService} is ready. Implementations may be
 * registered in {@code spring.factories}. The following constructor arguments types are
 * supported:
 * <ul>
 * <li>{@link ClassLoader}</li>
 * <li>{@link Environment}</li>
 * <li>{@link Binder}</li>
 * </ul>
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface ServiceReadinessCheck {

	/**
	 * Checks whether the given {@code service} is ready.
	 * @param service service to check
	 * @throws ServiceNotReadyException if the service is not ready
	 */
	void check(RunningService service) throws ServiceNotReadyException;

}
