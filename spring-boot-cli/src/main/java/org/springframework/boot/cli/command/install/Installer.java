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

package org.springframework.boot.cli.command.install;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import joptsimple.OptionSet;

import org.springframework.boot.cli.command.options.CompilerOptionHandler;
import org.springframework.boot.cli.command.options.OptionSetGroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.cli.util.Log;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SystemPropertyUtils;

/**
 * Shared logic for the {@link InstallCommand} and {@link UninstallCommand}.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 */
class Installer {

	private final DependencyResolver dependencyResolver;

	private final Properties installCounts;

	Installer(OptionSet options, CompilerOptionHandler compilerOptionHandler)
			throws IOException {
		this(new GroovyGrabDependencyResolver(
				createCompilerConfiguration(options, compilerOptionHandler)));
	}

	Installer(DependencyResolver resolver) throws IOException {
		this.dependencyResolver = resolver;
		this.installCounts = loadInstallCounts();
	}

	private static GroovyCompilerConfiguration createCompilerConfiguration(
			OptionSet options, CompilerOptionHandler compilerOptionHandler) {
		List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
				.createDefaultRepositoryConfiguration();
		return new OptionSetGroovyCompilerConfiguration(options, compilerOptionHandler,
				repositoryConfiguration) {
			@Override
			public boolean isAutoconfigure() {
				return false;
			}
		};
	}

	private Properties loadInstallCounts() throws IOException {
		Properties properties = new Properties();
		File installed = getInstalled();
		if (installed.exists()) {
			FileReader reader = new FileReader(installed);
			properties.load(reader);
			reader.close();
		}
		return properties;
	}

	private void saveInstallCounts() throws IOException {
		FileWriter writer = new FileWriter(getInstalled());
		try {
			this.installCounts.store(writer, null);
		}
		finally {
			writer.close();
		}
	}

	public void install(List<String> artifactIdentifiers) throws Exception {
		File libDirectory = getDefaultLibDirectory();
		libDirectory.mkdirs();
		Log.info("Installing into: " + libDirectory);
		List<File> artifactFiles = this.dependencyResolver.resolve(artifactIdentifiers);
		for (File artifactFile : artifactFiles) {
			int installCount = getInstallCount(artifactFile);
			if (installCount == 0) {
				FileCopyUtils.copy(artifactFile,
						new File(libDirectory, artifactFile.getName()));
			}
			setInstallCount(artifactFile, installCount + 1);
		}
		saveInstallCounts();
	}

	private int getInstallCount(File file) {
		String countString = this.installCounts.getProperty(file.getName());
		if (countString == null) {
			return 0;
		}
		return Integer.valueOf(countString);
	}

	private void setInstallCount(File file, int count) {
		if (count == 0) {
			this.installCounts.remove(file.getName());
		}
		else {
			this.installCounts.setProperty(file.getName(), Integer.toString(count));
		}
	}

	public void uninstall(List<String> artifactIdentifiers) throws Exception {
		File libDirectory = getDefaultLibDirectory();
		Log.info("Uninstalling from: " + libDirectory);
		List<File> artifactFiles = this.dependencyResolver.resolve(artifactIdentifiers);
		for (File artifactFile : artifactFiles) {
			int installCount = getInstallCount(artifactFile);
			if (installCount <= 1) {
				new File(libDirectory, artifactFile.getName()).delete();
			}
			setInstallCount(artifactFile, installCount - 1);
		}
		saveInstallCounts();
	}

	public void uninstallAll() throws Exception {
		File libDirectory = getDefaultLibDirectory();
		Log.info("Uninstalling from: " + libDirectory);
		for (String name : this.installCounts.stringPropertyNames()) {
			new File(libDirectory, name).delete();
		}
		this.installCounts.clear();
		saveInstallCounts();
	}

	private File getDefaultLibDirectory() {
		String home = SystemPropertyUtils
				.resolvePlaceholders("${spring.home:${SPRING_HOME:.}}");
		return new File(home, "lib");
	}

	private File getInstalled() {
		return new File(getDefaultLibDirectory(), ".installed");
	}

}
