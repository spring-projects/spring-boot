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

package org.springframework.boot.developertools.restart;

import java.net.URL;

/**
 * Strategy interface used to initialize a {@link Restarter}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see DefaultRestartInitializer
 */
public interface RestartInitializer {

	/**
	 * {@link RestartInitializer} that doesn't return any URLs.
	 */
	public static final RestartInitializer NONE = new RestartInitializer() {

		@Override
		public URL[] getInitialUrls(Thread thread) {
			return null;
		}

	};

	/**
	 * Return the initial set of URLs for the {@link Restarter} or {@code null} if no
	 * initial restart is required.
	 * @param thread the source thread
	 * @return initial URLs or {@code null}
	 */
	URL[] getInitialUrls(Thread thread);

}
