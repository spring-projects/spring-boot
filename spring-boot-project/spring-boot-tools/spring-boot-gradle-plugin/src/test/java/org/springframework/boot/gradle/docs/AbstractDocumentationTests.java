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

import java.util.function.Supplier;

import org.junit.Rule;

import org.springframework.boot.gradle.testkit.GradleBuild;

/**
 * Base class for documentation tests.
 *
 * @author Jean-Baptiste Nizet
 */
public class AbstractDocumentationTests {

	@Rule
	public final GradleBuild gradleBuild;

	protected final String extension;

	protected AbstractDocumentationTests(DSL dsl) {
		this.gradleBuild = dsl.gradleBuildSupplier.get();
		this.extension = dsl.extension;
	}

	protected enum DSL {

		GROOVY(".gradle", GradleBuild::new), KOTLIN(".gradle.kts",
				() -> new GradleBuild().withMinimalGradleVersionForKotlinDSL());
		private final String extension;

		private final Supplier<GradleBuild> gradleBuildSupplier;

		DSL(String extension, Supplier<GradleBuild> gradleBuildSupplier) {
			this.extension = extension;
			this.gradleBuildSupplier = gradleBuildSupplier;
		}

	}

}
