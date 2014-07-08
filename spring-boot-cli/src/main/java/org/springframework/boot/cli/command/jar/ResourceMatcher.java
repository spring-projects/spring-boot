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

package org.springframework.boot.cli.command.jar;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * Used to match resources for inclusion in a CLI application's jar file
 *
 * @author Andy Wilkinson
 */
class ResourceMatcher {

	private static final String[] DEFAULT_INCLUDES = { "public/**", "resources/**",
			"static/**", "templates/**", "META-INF/**", "*" };

	private static final String[] DEFAULT_EXCLUDES = { ".*", "repository/**", "build/**",
			"target/**", "**/*.jar", "**/*.groovy" };

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private final List<String> includes;

	private final List<String> excludes;

	ResourceMatcher(List<String> includes, List<String> excludes) {
		this.includes = getOptions(includes, DEFAULT_INCLUDES);
		this.excludes = getOptions(excludes, DEFAULT_EXCLUDES);
	}

	public List<MatchedResource> find(List<File> roots) throws IOException {
		List<MatchedResource> matchedResources = new ArrayList<MatchedResource>();
		for (File root : roots) {
			if (root.isFile()) {
				matchedResources.add(new MatchedResource(root));
			}
			else {
				matchedResources.addAll(findInFolder(root));
			}
		}
		return matchedResources;
	}

	private List<MatchedResource> findInFolder(File folder) throws IOException {
		List<MatchedResource> matchedResources = new ArrayList<MatchedResource>();

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
				new FolderResourceLoader(folder));

		for (String include : this.includes) {
			for (Resource candidate : resolver.getResources(include)) {
				File file = candidate.getFile();
				if (file.isFile()) {
					MatchedResource matchedResource = new MatchedResource(folder, file);
					if (!isExcluded(matchedResource)) {
						matchedResources.add(matchedResource);
					}
				}
			}
		}

		return matchedResources;
	}

	private boolean isExcluded(MatchedResource matchedResource) {
		for (String exclude : this.excludes) {
			if (this.pathMatcher.match(exclude, matchedResource.getName())) {
				return true;
			}
		}
		return false;
	}

	private List<String> getOptions(List<String> values, String[] defaults) {
		Set<String> result = new LinkedHashSet<String>();
		Set<String> minus = new LinkedHashSet<String>();
		boolean deltasFound = false;
		for (String value : values) {
			if (value.startsWith("+")) {
				deltasFound = true;
				value = value.substring(1);
				result.add(value);
			}
			else if (value.startsWith("-")) {
				deltasFound = true;
				value = value.substring(1);
				minus.add(value);
			}
			else if (value.trim().length() > 0) {
				result.add(value);
			}
		}
		for (String value : defaults) {
			if (!minus.contains(value) || !deltasFound) {
				result.add(value);
			}
		}
		return new ArrayList<String>(result);
	}

	/**
	 * {@link ResourceLoader} to get load resource from a folder.
	 */
	private static class FolderResourceLoader extends DefaultResourceLoader {

		private final File rootFolder;

		public FolderResourceLoader(File root) throws MalformedURLException {
			super(new FolderClassLoader(root));
			this.rootFolder = root;
		}

		@Override
		protected Resource getResourceByPath(String path) {
			return new FileSystemResource(new File(this.rootFolder, path));
		}

	}

	/**
	 * {@link ClassLoader} backed by a folder.
	 */
	private static class FolderClassLoader extends URLClassLoader {

		public FolderClassLoader(File rootFolder) throws MalformedURLException {
			super(new URL[] { rootFolder.toURI().toURL() });
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return findResources(name);
		}

		@Override
		public URL getResource(String name) {
			return findResource(name);
		}

	}

	/**
	 * A single matched resource.
	 */
	public static final class MatchedResource {

		private final File file;

		private final String name;

		private final boolean root;

		private MatchedResource(File file) {
			this.name = file.getName();
			this.file = file;
			this.root = this.name.endsWith(".jar");
		}

		private MatchedResource(File rootFolder, File file) {
			this.name = StringUtils.cleanPath(file.getAbsolutePath().substring(
					rootFolder.getAbsolutePath().length() + 1));
			this.file = file;
			this.root = false;
		}

		private MatchedResource(File resourceFile, String path, boolean root) {
			this.file = resourceFile;
			this.name = path;
			this.root = root;
		}

		public String getName() {
			return this.name;
		}

		public File getFile() {
			return this.file;
		}

		public boolean isRoot() {
			return this.root;
		}

		@Override
		public String toString() {
			return this.file.getAbsolutePath();
		}

	}

}
