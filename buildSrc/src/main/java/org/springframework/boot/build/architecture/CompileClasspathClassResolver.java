/*
 * Copyright 2012-2025 the original author or authors.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.ArchUnitException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link ClassResolver} that resolves Java classes from a provided compile classpath.
 *
 * @author Dmytro Nosan
 */
class CompileClasspathClassResolver implements ClassResolver {

	static final String PROPERTY_NAME = CompileClasspathClassResolver.class.getName();

	private ClassUriImporter classUriImporter;

	private final URLClassLoader classLoader;

	CompileClasspathClassResolver() {
		this.classLoader = new URLClassLoader(getUrls(), getClass().getClassLoader());
	}

	@Override
	public void setClassUriImporter(ClassUriImporter classUriImporter) {
		this.classUriImporter = classUriImporter;
	}

	@Override
	public Optional<JavaClass> tryResolve(String typeName) {
		String fileName = typeName.replace(".", "/") + ".class";
		URL url = this.classLoader.getResource(fileName);
		if (url == null) {
			return Optional.empty();
		}
		try {
			return this.classUriImporter.tryImport(url.toURI());
		}
		catch (URISyntaxException ex) {
			throw new ArchUnitException.LocationException(ex);
		}
	}

	private static URL[] getUrls() {
		ArchConfiguration configuration = ArchConfiguration.get();
		String classpath = configuration.getProperty(PROPERTY_NAME);
		Assert.state(classpath != null, () -> PROPERTY_NAME + " property is not set");
		return Arrays.stream(StringUtils.tokenizeToStringArray(classpath, File.pathSeparator))
			.map(File::new)
			.map(CompileClasspathClassResolver::toURL)
			.toArray(URL[]::new);
	}

	private static URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new ArchUnitException.LocationException(ex);
		}
	}

}
