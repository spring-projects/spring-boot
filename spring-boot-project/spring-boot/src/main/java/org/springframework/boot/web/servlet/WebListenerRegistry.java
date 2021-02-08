/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.servlet;

import javax.servlet.annotation.WebListener;

/**
 * A registry that holds {@link WebListener @WebListeners}.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
public interface WebListenerRegistry {

	/**
	 * Adds web listeners that will be registered with the servlet container.
	 * @param webListenerClassNames the class names of the web listeners
	 */
	void addWebListeners(String... webListenerClassNames);

}
