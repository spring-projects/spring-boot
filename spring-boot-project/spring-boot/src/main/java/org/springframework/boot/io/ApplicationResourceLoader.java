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

package org.springframework.boot.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ContextResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Class can be used to obtain {@link ResourceLoader ResourceLoaders} supporting
 * additional {@link ProtocolResolver ProtocolResolvers} registered in
 * {@code spring.factories}.
 * <p>
 * When not delegating to an existing resource loader, plain paths without a qualifier
 * will resolve to file system resources. This is different from
 * {@code DefaultResourceLoader}, which resolves unqualified paths to classpath resources.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.3.0
 */
public class ApplicationResourceLoader extends DefaultResourceLoader {

	/**
	 * Create a new {@code ApplicationResourceLoader}.
	 * @deprecated since 3.4.0 for removal in 4.0.0 in favor of {@link #get()}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public ApplicationResourceLoader() {
		this(null);
	}

	/**
	 * Create a new {@code ApplicationResourceLoader}.
	 * @param classLoader the {@link ClassLoader} to load class path resources with, or
	 * {@code null} for using the thread context class loader at the time of actual
	 * resource access
	 * @deprecated since 3.4.0 for removal in 4.0.0 in favor of {@link #get(ClassLoader)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public ApplicationResourceLoader(ClassLoader classLoader) {
		super(classLoader);
		SpringFactoriesLoader loader = SpringFactoriesLoader.forDefaultResourceLocation(classLoader);
		getProtocolResolvers().addAll(loader.load(ProtocolResolver.class));
	}

	@Override
	protected Resource getResourceByPath(String path) {
		return new ApplicationResource(path);
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}. The factories file will
	 * be resolved using the default class loader at the time this call is made. Resources
	 * will be resolved using the default class loader at the time they are resolved.
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get() {
		return get((ClassLoader) null);
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}. The factories files and
	 * resources will be resolved using the specified class loader.
	 * @param classLoader the class loader to use or {@code null} to use the default class
	 * loader
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(ClassLoader classLoader) {
		return get(classLoader, SpringFactoriesLoader.forDefaultResourceLocation(classLoader));
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}.
	 * @param classLoader the class loader to use or {@code null} to use the default class
	 * loader
	 * @param springFactoriesLoader the {@link SpringFactoriesLoader} used to load
	 * {@link ProtocolResolver ProtocolResolvers}
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(ClassLoader classLoader, SpringFactoriesLoader springFactoriesLoader) {
		return get(classLoader, springFactoriesLoader, null);
	}

	/**
	 * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
	 * ProtocolResolvers} registered in {@code spring.factories}.
	 * @param classLoader the class loader to use or {@code null} to use the default class
	 * loader
	 * @param springFactoriesLoader the {@link SpringFactoriesLoader} used to load
	 * {@link ProtocolResolver ProtocolResolvers}
	 * @param workingDirectory the working directory
	 * @return a {@link ResourceLoader} instance
	 * @since 3.5.0
	 */
	public static ResourceLoader get(ClassLoader classLoader, SpringFactoriesLoader springFactoriesLoader,
			Path workingDirectory) {
		return get(ApplicationFileSystemResourceLoader.get(classLoader, workingDirectory), springFactoriesLoader);
	}

	/**
	 * Return a {@link ResourceLoader} delegating to the given resource loader and
	 * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
	 * {@code spring.factories}. The factories file will be resolved using the default
	 * class loader at the time this call is made.
	 * @param resourceLoader the delegate resource loader
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(ResourceLoader resourceLoader) {
		return get(resourceLoader, false);
	}

	/**
	 * Return a {@link ResourceLoader} delegating to the given resource loader and
	 * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
	 * {@code spring.factories}. The factories file will be resolved using the default
	 * class loader at the time this call is made.
	 * @param resourceLoader the delegate resource loader
	 * @param preferFileResolution if file based resolution is preferred when a suitable
	 * {@link ResourceFilePathResolver} support the resource
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.1
	 */
	public static ResourceLoader get(ResourceLoader resourceLoader, boolean preferFileResolution) {
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		return get(resourceLoader, SpringFactoriesLoader.forDefaultResourceLocation(resourceLoader.getClassLoader()),
				preferFileResolution);
	}

	/**
	 * Return a {@link ResourceLoader} delegating to the given resource loader and
	 * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
	 * {@code spring.factories}.
	 * @param resourceLoader the delegate resource loader
	 * @param springFactoriesLoader the {@link SpringFactoriesLoader} used to load
	 * {@link ProtocolResolver ProtocolResolvers}
	 * @return a {@link ResourceLoader} instance
	 * @since 3.4.0
	 */
	public static ResourceLoader get(ResourceLoader resourceLoader, SpringFactoriesLoader springFactoriesLoader) {
		return get(resourceLoader, springFactoriesLoader, false);
	}

	private static ResourceLoader get(ResourceLoader resourceLoader, SpringFactoriesLoader springFactoriesLoader,
			boolean preferFileResolution) {
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
		List<ProtocolResolver> protocolResolvers = springFactoriesLoader.load(ProtocolResolver.class);
		List<ResourceFilePathResolver> filePathResolvers = (preferFileResolution)
				? springFactoriesLoader.load(ResourceFilePathResolver.class) : Collections.emptyList();
		return new ProtocolResolvingResourceLoader(resourceLoader, protocolResolvers, filePathResolvers);
	}

	/**
	 * Internal {@link ResourceLoader} used to load {@link ApplicationResource}.
	 */
	private static final class ApplicationFileSystemResourceLoader extends DefaultResourceLoader {

		private static final ResourceLoader shared = new ApplicationFileSystemResourceLoader(null, null);

		private final Path workingDirectory;

		private ApplicationFileSystemResourceLoader(ClassLoader classLoader, Path workingDirectory) {
			super(classLoader);
			this.workingDirectory = workingDirectory;
		}

		@Override
		public Resource getResource(String location) {
			Resource resource = super.getResource(location);
			if (this.workingDirectory == null) {
				return resource;
			}
			if (!resource.isFile()) {
				return resource;
			}
			return resolveFile(resource);
		}

		private Resource resolveFile(Resource resource) {
			try {
				File file = resource.getFile();
				return new ApplicationResource(this.workingDirectory.resolve(file.toPath()));
			}
			catch (FileNotFoundException ex) {
				return resource;
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		protected Resource getResourceByPath(String path) {
			return new ApplicationResource(path);
		}

		static ResourceLoader get(ClassLoader classLoader, Path workingDirectory) {
			if (classLoader == null && workingDirectory != null) {
				throw new IllegalArgumentException(
						"It's not possible to use null as 'classLoader' but specify a 'workingDirectory'");
			}
			return (classLoader != null) ? new ApplicationFileSystemResourceLoader(classLoader, workingDirectory)
					: ApplicationFileSystemResourceLoader.shared;
		}

	}

	/**
	 * An application {@link Resource}.
	 */
	private static final class ApplicationResource extends FileSystemResource implements ContextResource {

		ApplicationResource(String path) {
			super(path);
		}

		ApplicationResource(Path path) {
			super(path);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

	}

	/**
	 * {@link ResourceLoader} decorator that adds support for additional
	 * {@link ProtocolResolver ProtocolResolvers}.
	 */
	private static class ProtocolResolvingResourceLoader implements ResourceLoader {

		private final ResourceLoader resourceLoader;

		private final List<ProtocolResolver> protocolResolvers;

		private final List<ResourceFilePathResolver> filePathResolvers;

		ProtocolResolvingResourceLoader(ResourceLoader resourceLoader, List<ProtocolResolver> protocolResolvers,
				List<ResourceFilePathResolver> filePathResolvers) {
			this.resourceLoader = resourceLoader;
			this.protocolResolvers = protocolResolvers;
			this.filePathResolvers = filePathResolvers;
		}

		@Override
		public ClassLoader getClassLoader() {
			return this.resourceLoader.getClassLoader();
		}

		@Override
		public Resource getResource(String location) {
			if (StringUtils.hasLength(location)) {
				for (ProtocolResolver protocolResolver : this.protocolResolvers) {
					Resource resource = protocolResolver.resolve(location, this);
					if (resource != null) {
						return resource;
					}
				}
			}
			Resource resource = this.resourceLoader.getResource(location);
			String fileSystemPath = getFileSystemPath(location, resource);
			return (fileSystemPath != null) ? new ApplicationResource(fileSystemPath) : resource;
		}

		private String getFileSystemPath(String location, Resource resource) {
			for (ResourceFilePathResolver filePathResolver : this.filePathResolvers) {
				String filePath = filePathResolver.resolveFilePath(location, resource);
				if (filePath != null) {
					return filePath;
				}
			}
			return null;
		}

	}

}
