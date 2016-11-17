/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFileURLStreamHandler;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceFolder;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;

/**
 * A {@code ResourcePatternResolver} that considers {@link ClassLoaderFiles} when
 * resolving resources.
 *
 * @author Andy Wilkinson
 */
final class ClassLoaderFilesResourcePatternResolver implements ResourcePatternResolver {

	private static final Set<String> LOCATION_PATTERN_PREFIXES = Collections
			.unmodifiableSet(new HashSet<String>(
					Arrays.asList(CLASSPATH_ALL_URL_PREFIX, CLASSPATH_URL_PREFIX)));

	private final ResourcePatternResolver delegate = new PathMatchingResourcePatternResolver();

	private final AntPathMatcher antPathMatcher = new AntPathMatcher();

	private final ClassLoaderFiles classLoaderFiles;

	ClassLoaderFilesResourcePatternResolver(ClassLoaderFiles classLoaderFiles) {
		this.classLoaderFiles = classLoaderFiles;
	}

	@Override
	public Resource getResource(String location) {
		Resource candidate = this.delegate.getResource(location);
		if (isExcludedResource(candidate)) {
			return new DeletedClassLoaderFileResource(location);
		}
		return candidate;
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.delegate.getClassLoader();
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		List<Resource> resources = new ArrayList<Resource>();
		Resource[] candidates = this.delegate.getResources(locationPattern);
		for (Resource candidate : candidates) {
			if (!isExcludedResource(candidate)) {
				resources.add(candidate);
			}
		}
		resources.addAll(getAdditionalResources(locationPattern));
		return resources.toArray(new Resource[resources.size()]);
	}

	private String trimLocationPattern(String locationPattern) {
		for (String prefix : LOCATION_PATTERN_PREFIXES) {
			if (locationPattern.startsWith(prefix)) {
				return locationPattern.substring(prefix.length());
			}
		}
		return locationPattern;
	}

	private List<Resource> getAdditionalResources(String locationPattern)
			throws MalformedURLException {
		List<Resource> additionalResources = new ArrayList<Resource>();
		String trimmedLocationPattern = trimLocationPattern(locationPattern);
		for (SourceFolder sourceFolder : this.classLoaderFiles.getSourceFolders()) {
			for (Entry<String, ClassLoaderFile> entry : sourceFolder.getFilesEntrySet()) {
				if (entry.getValue().getKind() == Kind.ADDED && this.antPathMatcher
						.match(trimmedLocationPattern, entry.getKey())) {
					additionalResources.add(new UrlResource(new URL("reloaded", null, -1,
							"/" + entry.getKey(),
							new ClassLoaderFileURLStreamHandler(entry.getValue()))));
				}
			}
		}
		return additionalResources;
	}

	private boolean isExcludedResource(Resource resource) {
		for (SourceFolder sourceFolder : this.classLoaderFiles.getSourceFolders()) {
			for (Entry<String, ClassLoaderFile> entry : sourceFolder.getFilesEntrySet()) {
				try {
					if (entry.getValue().getKind() == Kind.DELETED && resource.exists()
							&& resource.getURI().toString().endsWith(entry.getKey())) {
						return true;
					}
				}
				catch (IOException ex) {
					throw new IllegalStateException(
							"Failed to retrieve URI from '" + resource + "'", ex);
				}
			}
		}
		return false;
	}

	/**
	 * A {@link Resource} that represents a {@link ClassLoaderFile} that has been
	 * {@link Kind#DELETED deleted}.
	 *
	 * @author Andy Wilkinson
	 */
	private final class DeletedClassLoaderFileResource extends AbstractResource {

		private final String name;

		private DeletedClassLoaderFileResource(String name) {
			this.name = name;
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public String getDescription() {
			return "Deleted: " + this.name;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException(this.name + " has been deleted");
		}
	}
}
