/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.springframework.boot.dependency.tools.Dependencies;
import org.springframework.boot.dependency.tools.ManagedDependencies;
import org.springframework.boot.dependency.tools.PropertiesFileDependencies;

/**
 * Utility to provide access to {@link ManagedDependencies} with support for version
 * file overrides.
 *
 * @author Phillip Webb
 */
public class VersionManagedDependencies {

	public static final String CONFIGURATION = "versionManagement";

	private Configuration versionManagementConfiguration;

	private Collection<Dependencies> versionManagedDependencies;

	private ManagedDependencies managedDependencies;

	public VersionManagedDependencies(Project project) {
		this.versionManagementConfiguration = project.getConfigurations().getByName(
				CONFIGURATION);
	}

	public ManagedDependencies getManagedDependencies() {
		if (this.managedDependencies == null) {
			this.managedDependencies = ManagedDependencies
					.get(getVersionManagedDependencies());
		}
		return this.managedDependencies;
	}

	private Collection<Dependencies> getVersionManagedDependencies() {
		if (versionManagedDependencies == null) {
			Set<File> files = versionManagementConfiguration.resolve();
			List<Dependencies> dependencies = new ArrayList<Dependencies>(files.size());
			for (File file : files) {
				dependencies.add(getPropertiesFileManagedDependencies(file));
			}
			this.versionManagedDependencies = dependencies;
		}
		return versionManagedDependencies;
	}

	private Dependencies getPropertiesFileManagedDependencies(File file) {
		if (!file.getName().toLowerCase().endsWith(".properties")) {
			throw new IllegalStateException(file + " is not a version property file");
		}
		try {
			return new PropertiesFileDependencies(new FileInputStream(file));
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
