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
import java.util.List;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.AntPathMatcher;

/**
 * Used to match resources for inclusion in a CLI application's jar file
 * 
 * @author Andy Wilkinson
 */
final class ResourceMatcher {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private final List<String> includes;

	private final List<String> excludes;

	ResourceMatcher(List<String> includes, List<String> excludes) {
		this.includes = includes;
		this.excludes = excludes;
	}

	List<MatchedResource> matchResources(List<File> roots) throws IOException {
		List<MatchedResource> matchedResources = new ArrayList<MatchedResource>();

		for (File root : roots) {
			if (root.isFile()) {
				matchedResources.add(new MatchedResource(root));
			}
			else {
				matchedResources.addAll(matchResources(root));
			}
		}
		return matchedResources;
	}

	private List<MatchedResource> matchResources(File root) throws IOException {
		List<MatchedResource> resources = new ArrayList<MatchedResource>();

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
				new ResourceCollectionResourceLoader(root));

		for (String include : this.includes) {
			Resource[] candidates = resolver.getResources(include);
			for (Resource candidate : candidates) {
				File file = candidate.getFile();
				if (file.isFile()) {
					MatchedResource matchedResource = new MatchedResource(root, file);
					if (!isExcluded(matchedResource)) {
						resources.add(matchedResource);
					}
				}
			}
		}

		return resources;
	}

	private boolean isExcluded(MatchedResource matchedResource) {
		for (String exclude : this.excludes) {
			if (this.pathMatcher.match(exclude, matchedResource.getPath())) {
				return true;
			}
		}

		return false;
	}

	private static final class ResourceCollectionResourceLoader extends
			DefaultResourceLoader {

		private final File root;

		ResourceCollectionResourceLoader(File root) throws MalformedURLException {
			super(new URLClassLoader(new URL[] { root.toURI().toURL() }) {
				@Override
				public Enumeration<URL> getResources(String name) throws IOException {
					return findResources(name);
				}

				@Override
				public URL getResource(String name) {
					return findResource(name);
				}
			});
			this.root = root;
		}

		@Override
		protected Resource getResourceByPath(String path) {
			return new FileSystemResource(new File(this.root, path));
		}
	}

	static final class MatchedResource {

		private final File file;

		private final String path;

		private final boolean root;

		private MatchedResource(File resourceFile) {
			this(resourceFile, resourceFile.getName(), true);
		}

		private MatchedResource(File root, File resourceFile) {
			this(resourceFile, resourceFile.getAbsolutePath().substring(
					root.getAbsolutePath().length() + 1), false);
		}

		private MatchedResource(File resourceFile, String path, boolean root) {
			this.file = resourceFile;
			this.path = path;
			this.root = root;
		}

		File getFile() {
			return this.file;
		}

		String getPath() {
			return this.path;
		}

		boolean isRoot() {
			return this.root;
		}

		@Override
		public String toString() {
			return this.file.getAbsolutePath();
		}
	}

}
