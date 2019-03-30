/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.Context;

/**
 * Callback interface that can be used to customize a Tomcat {@link Context}.
 *
 * @author Dave Syer
 * @see TomcatServletWebServerFactory
 * @since 2.0.0
 */
@FunctionalInterface
public interface TomcatContextCustomizer {

	/**
	 * Customize the context.
	 * @param context the context to customize
	 */
	void customize(Context context);

}
