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

package org.springframework.boot.web.servlet.error;

import org.springframework.stereotype.Controller;

/**
 * Marker interface used to identify a {@link Controller @Controller} that should be used
 * to render errors.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 */
@FunctionalInterface
public interface ErrorController {

	/**
	 * The return value from this method is not used; the property `server.error.path`
	 * must be set to override the default error page path.
	 * @return the error path
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of setting the property
	 * `server.error.path`
	 */
	@Deprecated
	String getErrorPath();

}
