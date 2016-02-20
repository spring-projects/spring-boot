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

package org.springframework.boot.gradle.dependencymanagement;

import groovy.lang.Closure;
import io.spring.gradle.dependencymanagement.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.gradle.api.Project;

import org.springframework.boot.gradle.PluginFeatures;
import org.springframework.util.ReflectionUtils;

/**
 * {@link PluginFeatures} to configure dependency management.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DependencyManagementPluginFeatures implements PluginFeatures {

	private static final String SPRING_BOOT_VERSION = DependencyManagementPluginFeatures.class
			.getPackage().getImplementationVersion();

	private static final String SPRING_BOOT_BOM = "org.springframework.boot:spring-boot-starter-parent:"
			+ SPRING_BOOT_VERSION;

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(DependencyManagementPlugin.class);
		DependencyManagementExtension dependencyManagement = project.getExtensions()
				.findByType(DependencyManagementExtension.class);
		dependencyManagement.imports(new Closure<Void>(this) {

			@Override
			public Void call(Object... args) {
				try {
					ReflectionUtils.findMethod(getDelegate().getClass(), "mavenBom",
							String.class).invoke(getDelegate(), SPRING_BOOT_BOM);
					return null;
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

		});
	}

}
