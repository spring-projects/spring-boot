/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.gradle.junit;

import java.lang.reflect.Field;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.testsupport.gradle.testkit.GradleBuild;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeforeEachCallback} to set a test class's {@code gradleBuild} field prior to
 * test execution.
 *
 * @author Andy Wilkinson
 */
final class GradleBuildFieldSetter implements BeforeEachCallback {

	private final GradleBuild gradleBuild;

	GradleBuildFieldSetter(GradleBuild gradleBuild) {
		this.gradleBuild = gradleBuild;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Field field = ReflectionUtils.findField(context.getRequiredTestClass(), "gradleBuild");
		field.setAccessible(true);
		field.set(context.getRequiredTestInstance(), this.gradleBuild);

	}

}
