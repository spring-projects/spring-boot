/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.architecture;

import java.util.HashMap;
import java.util.Map;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * Finds all configurations from auto-configurations (either nested configurations or
 * imported ones) and checks that these classes don't contain public members.
 * <p>
 * Kotlin classes are ignored as Kotlin does not have package-private visibility and
 * {@code internal} isn't a good substitute.
 *
 * @author Moritz Halbritter
 */
class AutoConfigurationChecker {

	private final DescribedPredicate<JavaClass> isAutoConfiguration = ArchitectureRules.areRegularAutoConfiguration()
		.and(ArchitectureRules.areNotKotlinClasses());

	EvaluationResult check(JavaClasses javaClasses) {
		AutoConfigurations autoConfigurations = new AutoConfigurations(javaClasses);
		for (JavaClass javaClass : javaClasses) {
			if (isAutoConfigurationOrEnclosedInAutoConfiguration(javaClass)) {
				autoConfigurations.add(javaClass);
			}
		}
		return ArchitectureRules.shouldHaveNoPublicMembers().evaluate(autoConfigurations.getConfigurations());
	}

	private boolean isAutoConfigurationOrEnclosedInAutoConfiguration(JavaClass javaClass) {
		if (this.isAutoConfiguration.test(javaClass)) {
			return true;
		}
		JavaClass enclosingClass = javaClass.getEnclosingClass().orElse(null);
		while (enclosingClass != null) {
			if (this.isAutoConfiguration.test(enclosingClass)) {
				return true;
			}
			enclosingClass = enclosingClass.getEnclosingClass().orElse(null);
		}
		return false;
	}

	private static final class AutoConfigurations {

		private static final String SPRING_BOOT_ROOT_PACKAGE = "org.springframework.boot";

		private static final String IMPORT = "org.springframework.context.annotation.Import";

		private static final String CONFIGURATION = "org.springframework.context.annotation.Configuration";

		private final JavaClasses classes;

		private final Map<String, JavaClass> autoConfigurationClasses = new HashMap<>();

		AutoConfigurations(JavaClasses classes) {
			this.classes = classes;
		}

		void add(JavaClass autoConfiguration) {
			if (!autoConfiguration.isMetaAnnotatedWith(CONFIGURATION)) {
				return;
			}
			if (this.autoConfigurationClasses.putIfAbsent(autoConfiguration.getName(), autoConfiguration) != null) {
				return;
			}
			processImports(autoConfiguration, this.autoConfigurationClasses);
		}

		JavaClasses getConfigurations() {
			DescribedPredicate<JavaClass> isAutoConfiguration = DescribedPredicate.describe("is an auto-configuration",
					(c) -> this.autoConfigurationClasses.containsKey(c.getName()));
			return this.classes.that(isAutoConfiguration);
		}

		private void processImports(JavaClass javaClass, Map<String, JavaClass> result) {
			JavaClass[] importedClasses = getImportedClasses(javaClass);
			for (JavaClass importedClass : importedClasses) {
				if (!isBootClass(importedClass)) {
					continue;
				}
				if (result.putIfAbsent(importedClass.getName(), importedClass) != null) {
					continue;
				}
				// Recursively find other imported classes
				processImports(importedClass, result);
			}
		}

		private JavaClass[] getImportedClasses(JavaClass javaClass) {
			if (!javaClass.isAnnotatedWith(IMPORT)) {
				return new JavaClass[0];
			}
			JavaAnnotation<JavaClass> imports = javaClass.getAnnotationOfType(IMPORT);
			return (JavaClass[]) imports.get("value").orElse(new JavaClass[0]);
		}

		private boolean isBootClass(JavaClass javaClass) {
			String pkg = javaClass.getPackage().getName();
			return pkg.equals(SPRING_BOOT_ROOT_PACKAGE) || pkg.startsWith(SPRING_BOOT_ROOT_PACKAGE + ".");
		}

	}

}
