/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFileURLStreamHandler;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceDirectory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
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

	private static final String[] LOCATION_PATTERN_PREFIXES = { CLASSPATH_ALL_URL_PREFIX, CLASSPATH_URL_PREFIX };

	private static final String WEB_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

	private final ResourcePatternResolver patternResolverDelegate;

	private final PathMatcher antPathMatcher = new AntPathMatcher();

	private final ClassLoaderFiles classLoaderFiles;

	/**
	 * Constructs a new ClassLoaderFilesResourcePatternResolver with the given
	 * applicationContext and classLoaderFiles.
	 * @param applicationContext the AbstractApplicationContext to be used
	 * @param classLoaderFiles the ClassLoaderFiles to be used
	 */
	ClassLoaderFilesResourcePatternResolver(AbstractApplicationContext applicationContext,
			ClassLoaderFiles classLoaderFiles) {
		this.classLoaderFiles = classLoaderFiles;
		this.patternResolverDelegate = getResourcePatternResolverFactory()
			.getResourcePatternResolver(applicationContext, retrieveResourceLoader(applicationContext));
	}

	/**
	 * Retrieves the ResourceLoader from the given ApplicationContext.
	 * @param applicationContext the ApplicationContext from which to retrieve the
	 * ResourceLoader
	 * @return the ResourceLoader if found, null otherwise
	 */
	private ResourceLoader retrieveResourceLoader(ApplicationContext applicationContext) {
		Field field = ReflectionUtils.findField(applicationContext.getClass(), "resourceLoader", ResourceLoader.class);
		if (field == null) {
			return null;
		}
		ReflectionUtils.makeAccessible(field);
		return (ResourceLoader) ReflectionUtils.getField(field, applicationContext);
	}

	/**
	 * Returns the appropriate ResourcePatternResolverFactory based on the presence of the
	 * WEB_CONTEXT_CLASS. If the WEB_CONTEXT_CLASS is present, it returns a new instance
	 * of WebResourcePatternResolverFactory. Otherwise, it returns a new instance of
	 * ResourcePatternResolverFactory.
	 * @return the appropriate ResourcePatternResolverFactory
	 */
	private ResourcePatternResolverFactory getResourcePatternResolverFactory() {
		if (ClassUtils.isPresent(WEB_CONTEXT_CLASS, null)) {
			return new WebResourcePatternResolverFactory();
		}
		return new ResourcePatternResolverFactory();
	}

	/**
	 * Returns the class loader used by this ClassLoaderFilesResourcePatternResolver.
	 * @return the class loader used by this ClassLoaderFilesResourcePatternResolver
	 */
	@Override
	public ClassLoader getClassLoader() {
		return this.patternResolverDelegate.getClassLoader();
	}

	/**
	 * Retrieves a resource based on the given location.
	 * @param location the location of the resource
	 * @return the resource object
	 */
	@Override
	public Resource getResource(String location) {
		Resource candidate = this.patternResolverDelegate.getResource(location);
		if (isDeleted(candidate)) {
			return new DeletedClassLoaderFileResource(location);
		}
		return candidate;
	}

	/**
	 * Retrieves an array of resources matching the given location pattern.
	 * @param locationPattern the location pattern to match resources against
	 * @return an array of resources matching the location pattern
	 * @throws IOException if an I/O error occurs while retrieving the resources
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		List<Resource> resources = new ArrayList<>();
		Resource[] candidates = this.patternResolverDelegate.getResources(locationPattern);
		for (Resource candidate : candidates) {
			if (!isDeleted(candidate)) {
				resources.add(candidate);
			}
		}
		resources.addAll(getAdditionalResources(locationPattern));
		return resources.toArray(new Resource[0]);
	}

	/**
	 * Retrieves additional resources based on the given location pattern.
	 * @param locationPattern the location pattern to match against
	 * @return a list of additional resources matching the location pattern
	 * @throws MalformedURLException if the URL for a resource is malformed
	 */
	private List<Resource> getAdditionalResources(String locationPattern) throws MalformedURLException {
		List<Resource> additionalResources = new ArrayList<>();
		String trimmedLocationPattern = trimLocationPattern(locationPattern);
		for (SourceDirectory sourceDirectory : this.classLoaderFiles.getSourceDirectories()) {
			for (Entry<String, ClassLoaderFile> entry : sourceDirectory.getFilesEntrySet()) {
				String name = entry.getKey();
				ClassLoaderFile file = entry.getValue();
				if (file.getKind() != Kind.DELETED && this.antPathMatcher.match(trimmedLocationPattern, name)) {
					URL url = new URL("reloaded", null, -1, "/" + name, new ClassLoaderFileURLStreamHandler(file));
					UrlResource resource = new UrlResource(url);
					additionalResources.add(resource);
				}
			}
		}
		return additionalResources;
	}

	/**
	 * Trims the location pattern by removing the prefix if it matches any of the
	 * predefined prefixes.
	 * @param pattern the location pattern to be trimmed
	 * @return the trimmed location pattern
	 */
	private String trimLocationPattern(String pattern) {
		for (String prefix : LOCATION_PATTERN_PREFIXES) {
			if (pattern.startsWith(prefix)) {
				return pattern.substring(prefix.length());
			}
		}
		return pattern;
	}

	/**
	 * Checks if a resource has been deleted.
	 * @param resource the resource to check
	 * @return true if the resource has been deleted, false otherwise
	 * @throws IllegalStateException if failed to retrieve URI from the resource
	 */
	private boolean isDeleted(Resource resource) {
		for (SourceDirectory sourceDirectory : this.classLoaderFiles.getSourceDirectories()) {
			for (Entry<String, ClassLoaderFile> entry : sourceDirectory.getFilesEntrySet()) {
				try {
					String name = entry.getKey();
					ClassLoaderFile file = entry.getValue();
					if (file.getKind() == Kind.DELETED && resource.exists()
							&& resource.getURI().toString().endsWith(name)) {
						return true;
					}
				}
				catch (IOException ex) {
					throw new IllegalStateException("Failed to retrieve URI from '" + resource + "'", ex);
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

		/**
		 * Constructs a new DeletedClassLoaderFileResource with the specified name.
		 * @param name the name of the resource
		 */
		private DeletedClassLoaderFileResource(String name) {
			this.name = name;
		}

		/**
		 * Returns a boolean value indicating whether the file exists.
		 * @return {@code true} if the file exists, {@code false} otherwise.
		 */
		@Override
		public boolean exists() {
			return false;
		}

		/**
		 * Returns the description of the DeletedClassLoaderFileResource.
		 * @return the description of the DeletedClassLoaderFileResource
		 */
		@Override
		public String getDescription() {
			return "Deleted: " + this.name;
		}

		/**
		 * Returns an input stream for reading the contents of this deleted class loader
		 * file resource.
		 * @return an input stream for reading the contents of this deleted class loader
		 * file resource.
		 * @throws IOException if an I/O error occurs.
		 */
		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException(this.name + " has been deleted");
		}

	}

	/**
	 * Factory used to create the {@link ResourcePatternResolver} delegate.
	 */
	private static class ResourcePatternResolverFactory {

		/**
		 * Returns a {@link ResourcePatternResolver} instance based on the provided
		 * {@link AbstractApplicationContext} and {@link ResourceLoader}.
		 * @param applicationContext the {@link AbstractApplicationContext} to use for
		 * retrieving protocol resolvers
		 * @param resourceLoader the {@link ResourceLoader} to use for loading resources
		 * @return a {@link ResourcePatternResolver} instance
		 */
		ResourcePatternResolver getResourcePatternResolver(AbstractApplicationContext applicationContext,
				ResourceLoader resourceLoader) {
			ResourceLoader targetResourceLoader = (resourceLoader != null) ? resourceLoader
					: new ApplicationContextResourceLoader(applicationContext::getProtocolResolvers);
			return new PathMatchingResourcePatternResolver(targetResourceLoader);
		}

	}

	/**
	 * {@link ResourcePatternResolverFactory} to be used when the classloader can access
	 * {@link WebApplicationContext}.
	 */
	private static final class WebResourcePatternResolverFactory extends ResourcePatternResolverFactory {

		/**
		 * Returns the resource pattern resolver based on the type of application context.
		 * If the application context is a WebApplicationContext, it returns the servlet
		 * context resource pattern resolver. Otherwise, it calls the super method to get
		 * the resource pattern resolver.
		 * @param applicationContext the application context
		 * @param resourceLoader the resource loader
		 * @return the resource pattern resolver
		 */
		@Override
		public ResourcePatternResolver getResourcePatternResolver(AbstractApplicationContext applicationContext,
				ResourceLoader resourceLoader) {
			if (applicationContext instanceof WebApplicationContext) {
				return getServletContextResourcePatternResolver(applicationContext, resourceLoader);
			}
			return super.getResourcePatternResolver(applicationContext, resourceLoader);
		}

		/**
		 * Returns a {@link ResourcePatternResolver} that resolves resources in the
		 * servlet context.
		 * @param applicationContext the application context
		 * @param resourceLoader the resource loader
		 * @return a {@link ResourcePatternResolver} for servlet context resources
		 */
		private ResourcePatternResolver getServletContextResourcePatternResolver(
				AbstractApplicationContext applicationContext, ResourceLoader resourceLoader) {
			ResourceLoader targetResourceLoader = (resourceLoader != null) ? resourceLoader
					: new WebApplicationContextResourceLoader(applicationContext::getProtocolResolvers,
							(WebApplicationContext) applicationContext);
			return new ServletContextResourcePatternResolver(targetResourceLoader);
		}

	}

	/**
	 * ApplicationContextResourceLoader class.
	 */
	private static class ApplicationContextResourceLoader extends DefaultResourceLoader {

		private final Supplier<Collection<ProtocolResolver>> protocolResolvers;

		/**
		 * Constructs a new ApplicationContextResourceLoader with the specified protocol
		 * resolvers.
		 * @param protocolResolvers the supplier of a collection of protocol resolvers to
		 * be used by this resource loader
		 */
		ApplicationContextResourceLoader(Supplier<Collection<ProtocolResolver>> protocolResolvers) {
			super(null);
			this.protocolResolvers = protocolResolvers;
		}

		/**
		 * Returns a collection of protocol resolvers.
		 * @return the collection of protocol resolvers
		 */
		@Override
		public Collection<ProtocolResolver> getProtocolResolvers() {
			return this.protocolResolvers.get();
		}

	}

	/**
	 * {@link ResourceLoader} that optionally supports {@link ServletContextResource
	 * ServletContextResources}.
	 */
	private static class WebApplicationContextResourceLoader extends ApplicationContextResourceLoader {

		private final WebApplicationContext applicationContext;

		/**
		 * Constructs a new WebApplicationContextResourceLoader with the specified
		 * protocol resolvers and web application context.
		 * @param protocolResolvers the supplier of protocol resolvers to be used by this
		 * resource loader
		 * @param applicationContext the web application context to be used by this
		 * resource loader
		 */
		WebApplicationContextResourceLoader(Supplier<Collection<ProtocolResolver>> protocolResolvers,
				WebApplicationContext applicationContext) {
			super(protocolResolvers);
			this.applicationContext = applicationContext;
		}

		/**
		 * Retrieves a resource by its path.
		 * @param path the path of the resource
		 * @return the resource object
		 */
		@Override
		protected Resource getResourceByPath(String path) {
			if (this.applicationContext.getServletContext() != null) {
				return new ServletContextResource(this.applicationContext.getServletContext(), path);
			}
			return super.getResourceByPath(path);
		}

	}

}
