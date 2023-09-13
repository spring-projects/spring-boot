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

package org.springframework.boot.autoconfigure.thread;

import org.springframework.boot.system.JavaVersion;
import org.springframework.core.env.Environment;

/**
 * Threading of the application.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public enum Threading {

	/**
	 * Platform threads. Active if virtual threads are not active.
	 */
	PLATFORM {

		@Override
		public boolean isActive(Environment environment) {
			return !VIRTUAL.isActive(environment);
		}

	},
	/**
	 * Virtual threads. Active if {@code spring.threads.virtual.enabled} is {@code true}
	 * and running on Java 21 or later.
	 */
	VIRTUAL {

		@Override
		public boolean isActive(Environment environment) {
			return environment.getProperty("spring.threads.virtual.enabled", boolean.class, false)
					&& JavaVersion.getJavaVersion().isEqualOrNewerThan(JavaVersion.TWENTY_ONE);
		}

	};

	/**
	 * Determines whether the threading is active.
	 * @param environment the environment
	 * @return whether the threading is active
	 */
	public abstract boolean isActive(Environment environment);

}
