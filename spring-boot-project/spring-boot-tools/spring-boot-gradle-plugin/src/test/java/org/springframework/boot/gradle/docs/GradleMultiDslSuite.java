/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.gradle.docs;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Rule;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import org.springframework.boot.gradle.testkit.GradleBuild;

/**
 * Custom {@link Suite} that runs tests against the Groovy and the Kotlin DSLs. Test
 * classes using the suite must have a public {@link DSL} field named {@code dsl} and a
 * public {@link GradleBuild} field named {@code gradleBuild} and annotated with
 * {@link Rule}
 *
 * @author Jean-Baptiste Nizet
 */
public final class GradleMultiDslSuite extends Suite {

	public GradleMultiDslSuite(Class<?> clazz) throws InitializationError {
		super(clazz, createRunners(clazz));
	}

	private static List<Runner> createRunners(Class<?> clazz) throws InitializationError {
		List<Runner> runners = new ArrayList<>();
		runners.add(new GradleDslClassRunner(clazz, new GradleBuild(), DSL.GROOVY));
		runners.add(new GradleDslClassRunner(clazz,
				new GradleBuild().withMinimalGradleVersionForKotlinDSL(), DSL.KOTLIN));
		return runners;
	}

	private static final class GradleDslClassRunner extends BlockJUnit4ClassRunner {

		private final GradleBuild gradleBuild;

		private final DSL dsl;

		private GradleDslClassRunner(Class<?> klass, GradleBuild gradleBuild, DSL dsl)
				throws InitializationError {
			super(klass);
			this.gradleBuild = gradleBuild;
			this.dsl = dsl;
		}

		@Override
		protected Object createTest() throws Exception {
			Object test = super.createTest();
			configureTest(test);
			return test;
		}

		private void configureTest(Object test) throws Exception {
			test.getClass().getField("gradleBuild").set(test, this.gradleBuild);
			test.getClass().getField("dsl").set(test, this.dsl);
		}

		@Override
		protected String getName() {
			return this.dsl.getName() + " DSL";
		}

		@Override
		protected String testName(FrameworkMethod method) {
			return method.getName() + " [" + getName() + "]";
		}

	}

}
