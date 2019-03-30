/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFileURLStreamHandler;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceFolder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * A {@code ResourcePatternResolver} that considers {@link ClassLoaderFiles} when
 * resolving resources.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
final class ClassLoaderFilesResourcePatternResolver implements ResourcePatternResolver {

	private static final String[] LOCATION_PATTERN_PREFIXES = { CLASSPATH_ALL_URL_PREFIX,
			CLASSPATH_URL_PREFIX };

	private static final String WEB_CONTEXT_CLASS = "org.springframework.web.context."
			+ "WebApplicationContext";

	private final ResourcePatternResolver patternResolverDelegate;

	private final PathMatcher antPathMatcher = new AntPathMatcher();

	private final ClassLoaderFiles classLoaderFiles;

	ClassLoaderFilesResourcePatternResolver(ApplicationContext applicationContext,
			ClassLoaderFiles classLoaderFiles) {
		this.classLoaderFiles = classLoaderFiles;
		this.patternResolverDelegate = getResourcePatternResolverFactory()
				.getResourcePatternResolver(applicationContext,
						retrieveResourceLoader(applicationContext));
	}

	private ResourceLoader retrieveResourceLoader(ApplicationContext applicationContext) {
		Field field = ReflectionUtils.findField(applicationContext.getClass(),
				"resourceLoader", ResourceLoader.class);
		if (field == null) {
			return null;
		}
		ReflectionUtils.makeAccessible(field);
		return (ResourceLoader) ReflectionUtils.getField(field, applicationContext);
	}

	private ResourcePatternResolverFactory getResourcePatternResolverFactory() {
		if (ClassUtils.isPresent(WEB_CONTEXT_CLASS, null)) {
			return new WebResourcePatternResolverFactory();
		}
		return new ResourcePatternResolverFactory();
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.patternResolverDelegate.getClassLoader();
	}

	@Override
	public Resource getResource(String location) {
		Resource candidate = this.patternResolverDelegate.getResource(location);
		if (isDeleted(candidate)) {
			return new DeletedClassLoaderFileResource(location);
		}
		return candidate;
	}

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		List<Resource> resources = new ArrayList<>();
		Resource[] candidates = this.patternResolverDelegate
				.getResources(locationPattern);
		for (Resource candidate : candidates) {
			if (!isDeleted(candidate)) {
				resources.add(candidate);
			}
		}
		resources.addAll(getAdditionalResources(locationPattern));
		return resources.toArray(new Resource[0]);
	}

	private List<Resource> getAdditionalResources(String locationPattern)
			throws MalformedURLException {
		List<Resource> additionalResources = new ArrayList<>();
		String trimmedLocationPattern = trimLocationPattern(locationPattern);
		for (SourceFolder sourceFolder : this.classLoaderFiles.getSourceFolders()) {
			for (Entry<String, ClassLoaderFile> entry : sourceFolder.getFilesEntrySet()) {
				String name = entry.getKey();
				ClassLoaderFile file = entry.getValue();
				if (file.getKind() != Kind.DELETED
						&& this.antPathMatcher.match(trimmedLocationPattern, name)) {
					URL url = new URL("reloaded", null, -1, "/" + name,
							new ClassLoaderFileURLStreamHandler(file));
					UrlResource resource = new UrlResource(url);
					additionalResources.add(resource);
				}
			}
		}
		return additionalResources;
	}

	private String trimLocationPattern(String pattern) {
		for (String prefix : LOCATION_PATTERN_PREFIXES) {
			if (pattern.startsWith(prefix)) {
				return pattern.substring(prefix.length());
			}
		}
		return pattern;
	}

	private boolean isDeleted(Resource resource) {
		for (SourceFolder sourceFolder : this.classLoaderFiles.getSourceFolders()) {
			for (Entry<String, ClassLoaderFile> entry : sourceFolder.getFilesEntrySet()) {
				try {
					String name = entry.getKey();
					ClassLoaderFile file = entry.getValue();
					if (file.getKind() == Kind.DELETED && resource.exists()
							&& resource.getURI().toString().endsWith(name)) {
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
	 */
	static final class DeletedClassLoaderFileResource extends AbstractResource {

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

	/**
	 * Factory used to create the {@link ResourcePatternResolver} delegate.
	 */
	private static class ResourcePatternResolverFactory {

		public ResourcePatternResolver getResourcePatternResolver(
				ApplicationContext applicationContext, ResourceLoader resourceLoader) {
			if (resourceLoader == null) {
				resourceLoader = new DefaultResourceLoader();
				copyProtocolResolvers(applicationContext, resourceLoader);
			}
			return new PathMatchingResourcePatternResolver(resourceLoader);
		}

		protected final void copyProtocolResolvers(ApplicationContext applicationContext,
				ResourceLoader resourceLoader) {
			if (applicationContext instanceof DefaultResourceLoader
					&& resourceLoader instanceof DefaultResourceLoader) {
				copyProtocolResolvers((DefaultResourceLoader) applicationContext,
						(DefaultResourceLoader) resourceLoader);
			}
		}

		protected final void copyProtocolResolvers(DefaultResourceLoader source,
				DefaultResourceLoader destination) {
			for (ProtocolResolver resolver : source.getProtocolResolvers()) {
				destination.addProtocolResolver(resolver);
			}
		}

	}

	/**
	 * {@link ResourcePatternResolverFactory} to be used when the classloader can access
	 * {@link WebApplicationContext}.
	 */
	private static class WebResourcePatternResolverFactory
			extends ResourcePatternResolverFactory {

		@Override
		public ResourcePatternResolver getResourcePatternResolver(
				ApplicationContext applicationContext, ResourceLoader resourceLoader) {
			if (applicationContext instanceof WebApplicationContext) {
				return getResourcePatternResolver(
						(WebApplicationContext) applicationContext, resourceLoader);
			}
			return super.getResourcePatternResolver(applicationContext, resourceLoader);
		}

		private ResourcePatternResolver getResourcePatternResolver(
				WebApplicationContext applicationContext, ResourceLoader resourceLoader) {
			if (resourceLoader == null) {
				resourceLoader = new WebApplicationContextResourceLoader(
						applicationContext);
				copyProtocolResolvers(applicationContext, resourceLoader);
			}
			return new ServletContextResourcePatternResolver(resourceLoader);

		}

	}

	/**
	 * {@link ResourceLoader} that optionally supports {@link ServletContextResource
	 * ServletContextResources}.
	 */
	private static class WebApplicationContextResourceLoader
			extends DefaultResourceLoader {

		private final WebApplicationContext applicationContext;

		WebApplicationContextResourceLoader(WebApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		protected Resource getResourceByPath(String path) {
			if (this.applicationContext.getServletContext() != null) {
				return new ServletContextResource(
						this.applicationContext.getServletContext(), path);
			}
			return super.getResourceByPath(path);
		}

	}

}
