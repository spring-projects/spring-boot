/*
 * Copyright 2026 the original author or authors.
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

package org.springframework.boot.build;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * DSL extension for configuring a project's system requirements.
 *
 * @author Andy Wilkinson
 */
public class SystemRequirementsExtension {

	private final JavaSpec javaSpec;

	@Inject
	public SystemRequirementsExtension(Project project, ObjectFactory objects) {
		this.javaSpec = objects.newInstance(JavaSpec.class, project);
	}

	public void java(Action<JavaSpec> action) {
		action.execute(this.javaSpec);
	}

	public JavaSpec getJava() {
		return this.javaSpec;
	}

	public abstract static class JavaSpec {

		private final Project project;

		private int version = 17;

		@Inject
		public JavaSpec(Project project) {
			this.project = project;
		}

		public int getVersion() {
			return this.version;
		}

		public void setVersion(int version) {
			JavaLanguageVersion javaVersion = JavaLanguageVersion.of(version);
			JavaPluginExtension javaPluginExtension = this.project.getExtensions().getByType(JavaPluginExtension.class);
			javaPluginExtension.setSourceCompatibility(javaVersion);
			javaPluginExtension.setTargetCompatibility(javaVersion);
			this.version = version;
		}

	}

}
