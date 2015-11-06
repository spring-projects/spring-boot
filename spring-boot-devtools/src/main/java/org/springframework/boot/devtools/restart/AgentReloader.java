/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import org.springframework.util.ClassUtils;

/**
 * Utility to determine if an Java agent based reloader (e.g. JRebel) is being used.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public abstract class AgentReloader {

	private AgentReloader() {
	}

	/**
	 * Determine if any agent reloader is active.
	 * @return true if agent reloading is active
	 */
	public static boolean isActive() {
		return isJRebelActive();
	}

	/**
	 * Determine if JRebel is active.
	 * @return true if JRebel is active
	 */
	public static boolean isJRebelActive() {
		return ClassUtils.isPresent("org.zeroturnaround.javarebel.ReloaderFactory", null);
	}

}
