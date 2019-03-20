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

package org.springframework.boot.junit.runner.classpath;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModifiedClassPathRunner} overriding entries on the class path.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides("org.springframework:spring-context:4.1.0.RELEASE")
public class ModifiedClassPathRunnerOverridesTests {

	@Test
	public void classesAreLoadedFromOverride() {
		assertThat(ApplicationContext.class.getProtectionDomain().getCodeSource()
				.getLocation().toString()).endsWith("spring-context-4.1.0.RELEASE.jar");
	}

	@Test
	public void classesAreLoadedFromTransitiveDependencyOfOverride() {
		assertThat(StringUtils.class.getProtectionDomain().getCodeSource().getLocation()
				.toString()).endsWith("spring-core-4.1.0.RELEASE.jar");
	}

}
