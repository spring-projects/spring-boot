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

package org.springframework.boot.devtools.classpath;

import org.springframework.boot.devtools.filewatch.ChangedFile;

/**
 * Strategy interface used to determine when a changed classpath file should trigger a
 * full application restart. For example, static web resources might not require a full
 * restart where as class files would.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see PatternClassPathRestartStrategy
 */
@FunctionalInterface
public interface ClassPathRestartStrategy {

	/**
	 * Return true if a full restart is required.
	 * @param file the changed file
	 * @return {@code true} if a full restart is required
	 */
	boolean isRestartRequired(ChangedFile file);

}
