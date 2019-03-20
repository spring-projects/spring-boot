/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SpringBootPlugin} subclass that outputs a deprecation warning to direct people
 * to use the new Gradle Plugin Portal-compatible ID {@code org.springframework.boot}.
 *
 * @author Andy Wilkinson
 * @deprecated as of 1.4.2 in favor of {@link SpringBootPlugin}
 */
@Deprecated
public class DeprecatedSpringBootPlugin extends SpringBootPlugin {

	private static final Logger logger = LoggerFactory
			.getLogger(DeprecatedSpringBootPlugin.class);

	@Override
	public void apply(Project project) {
		logger.warn("The plugin id 'spring-boot' is deprecated. Please use "
				+ "'org.springframework.boot' instead.");
		super.apply(project);
	}

}
