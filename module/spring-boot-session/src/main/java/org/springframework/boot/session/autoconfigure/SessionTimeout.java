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

package org.springframework.boot.session.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * Provides access to the configured session timeout. The timeout is available
 * irrespective of the deployment type:
 * <ul>
 * <li>a servlet web application
 * <ul>
 * <li>using an embedded web server
 * <li>deployed as a war file
 * </ul>
 * <li>a reactive web application
 * </ul>
 *
 * @author Andy Wilkinson
 * @since 4.0.1
 */
@FunctionalInterface
public interface SessionTimeout {

	/**
	 * Returns the session timeout or {@code null} if no timeout has been configured.
	 * @return the timeout or {@code null}
	 */
	@Nullable Duration getTimeout();

}
