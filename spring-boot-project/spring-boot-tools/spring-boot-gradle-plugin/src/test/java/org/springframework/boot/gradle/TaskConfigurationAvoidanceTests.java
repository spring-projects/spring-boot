/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.gradle.api.Action;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;

/**
 * Tests that verify the plugin's compliance with task configuration avoidance.
 *
 * @author Andy Wilkinson
 */
@AnalyzeClasses(packages = "org.springframework.boot.gradle",
		importOptions = TaskConfigurationAvoidanceTests.DoNotIncludeTests.class)
class TaskConfigurationAvoidanceTests {

	@ArchTest
	void noApisThatCauseEagerTaskConfigurationShouldBeCalled(JavaClasses classes) {
		ProhibitedMethods prohibited = new ProhibitedMethods();
		prohibited.on(TaskContainer.class).methodsNamed("create", "findByPath, getByPath").method("withType",
				Class.class, Action.class);
		prohibited.on(TaskCollection.class).methodsNamed("findByName", "getByName");
		ArchRuleDefinition.noClasses().should()
				.callMethodWhere(DescribedPredicate.describe("it would cause eager task configuration", prohibited))
				.check(classes);
	}

	static class DoNotIncludeTests implements ImportOption {

		@Override
		public boolean includes(Location location) {
			return !location.matches(Pattern.compile(".*Tests\\.class"));
		}

	}

	private static final class ProhibitedMethods implements Predicate<JavaMethodCall> {

		private final List<Predicate<JavaMethodCall>> prohibited = new ArrayList<>();

		private ProhibitedConfigurer on(Class<?> type) {
			return new ProhibitedConfigurer(type);
		}

		@Override
		public boolean apply(JavaMethodCall methodCall) {
			for (Predicate<JavaMethodCall> spec : this.prohibited) {
				if (spec.apply(methodCall)) {
					return true;
				}
			}
			return false;
		}

		private final class ProhibitedConfigurer {

			private final Class<?> type;

			private ProhibitedConfigurer(Class<?> type) {
				this.type = type;
			}

			private ProhibitedConfigurer methodsNamed(String... names) {
				for (String name : names) {
					ProhibitedMethods.this.prohibited.add(new ProhibitMethodsNamed(this.type, name));
				}
				return this;
			}

			private ProhibitedConfigurer method(String name, Class<?>... parameterTypes) {
				ProhibitedMethods.this.prohibited
						.add(new ProhibitMethod(this.type, name, Arrays.asList(parameterTypes)));
				return this;
			}

		}

		static class ProhibitMethodsNamed implements Predicate<JavaMethodCall> {

			private final Class<?> owner;

			private final String name;

			ProhibitMethodsNamed(Class<?> owner, String name) {
				this.owner = owner;
				this.name = name;
			}

			@Override
			public boolean apply(JavaMethodCall methodCall) {
				return methodCall.getTargetOwner().isEquivalentTo(this.owner) && methodCall.getName().equals(this.name);
			}

		}

		private static final class ProhibitMethod extends ProhibitMethodsNamed {

			private final List<Class<?>> parameterTypes;

			private ProhibitMethod(Class<?> owner, String name, List<Class<?>> parameterTypes) {
				super(owner, name);
				this.parameterTypes = parameterTypes;
			}

			@Override
			public boolean apply(JavaMethodCall methodCall) {
				return super.apply(methodCall) && match(methodCall.getTarget().getParameterTypes());
			}

			private boolean match(List<JavaType> callParameterTypes) {
				if (this.parameterTypes.size() != callParameterTypes.size()) {
					return false;
				}
				for (int i = 0; i < this.parameterTypes.size(); i++) {
					if (!callParameterTypes.get(i).toErasure().isEquivalentTo(this.parameterTypes.get(i))) {
						return false;
					}
				}
				return true;
			}

		}

	}

}
