/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.servlet.error;

import org.springframework.stereotype.Controller;

/**
 * Marker interface used to indicate that a {@link Controller @Controller} is used to
 * render errors. Primarily used to know the error paths that will not need to be secured.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface ErrorController {

	/**
	 * Returns the path of the error page.
	 * @return the error path
	 */
	String getErrorPath();

}
