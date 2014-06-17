/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.test;


import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Application context related test utilities.
 *
 * @author Stephane Nicoll
 * @since 1.1.1
 */
public abstract class ApplicationContextTestUtils {

	/**
	 * Closes this {@link ApplicationContext} and its parent hierarchy
	 * if any.
	 * @param context the context to close (can be {@code null})
	 */
	public static void closeAll(ApplicationContext context) {
		if (context != null) {
			ApplicationContext parent = context.getParent();
			if (context instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) context).close();
			}
			closeAll(parent);
		}
	}

}
