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

package org.springframework.boot.test.autoconfigure.web.servlet;

/**
 * MVC print options specified from {@link AutoConfigureMockMvc @AutoConfigureMockMvc}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public enum MockMvcPrint {

	/**
	 * Use the default print setting ({@code MockMvcPrint.SYSTEM_OUT} unless explicitly
	 * overridden).
	 */
	DEFAULT,

	/**
	 * Log MVC interactions at the {@code DEBUG} level.
	 */
	LOG_DEBUG,

	/**
	 * Print MVC interactions to {@code System.out}.
	 */
	SYSTEM_OUT,

	/**
	 * Print MVC interactions to {@code System.err}.
	 */
	SYSTEM_ERR,

	/**
	 * Do not print MVC interactions.
	 */
	NONE

}
