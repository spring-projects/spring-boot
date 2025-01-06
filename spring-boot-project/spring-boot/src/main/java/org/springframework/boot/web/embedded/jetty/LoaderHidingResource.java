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

package org.springframework.boot.web.embedded.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.eclipse.jetty.util.resource.CombinedResource;
import org.eclipse.jetty.util.resource.Resource;

/**
 * A custom {@link Resource} that hides Spring Boot's loader classes, preventing them from
 * being served over HTTP.
 *
 * @author Andy Wilkinson
 */
final class LoaderHidingResource extends Resource {

	private static final String LOADER_RESOURCE_PATH_PREFIX = "/org/springframework/boot/";

	private final Path loaderBasePath;

	private final Resource base;

	private final Resource delegate;

	LoaderHidingResource(Resource base, Resource delegate) {
		this.base = base;
		this.delegate = delegate;
		this.loaderBasePath = base.getPath().getFileSystem().getPath("/", "org", "springframework", "boot");
	}

	@Override
	public void forEach(Consumer<? super Resource> action) {
		this.delegate.forEach(action);
	}

	@Override
	public Path getPath() {
		return this.delegate.getPath();
	}

	@Override
	public boolean isContainedIn(Resource r) {
		return this.delegate.isContainedIn(r);
	}

	@Override
	public Iterator<Resource> iterator() {
		if (this.delegate instanceof CombinedResource) {
			return list().iterator();
		}
		return List.<Resource>of(this).iterator();
	}

	@Override
	public boolean equals(Object obj) {
		return this.delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public boolean exists() {
		return this.delegate.exists();
	}

	@Override
	public Spliterator<Resource> spliterator() {
		return this.delegate.spliterator();
	}

	@Override
	public boolean isDirectory() {
		return this.delegate.isDirectory();
	}

	@Override
	public boolean isReadable() {
		return this.delegate.isReadable();
	}

	@Override
	public Instant lastModified() {
		return this.delegate.lastModified();
	}

	@Override
	public long length() {
		return this.delegate.length();
	}

	@Override
	public URI getURI() {
		return this.delegate.getURI();
	}

	@Override
	public String getName() {
		return this.delegate.getName();
	}

	@Override
	public String getFileName() {
		return this.delegate.getFileName();
	}

	@Override
	public InputStream newInputStream() throws IOException {
		return this.delegate.newInputStream();
	}

	@Override
	@SuppressWarnings({ "deprecation", "removal" })
	public ReadableByteChannel newReadableByteChannel() throws IOException {
		return this.delegate.newReadableByteChannel();
	}

	@Override
	public List<Resource> list() {
		return asLoaderHidingResources(this.delegate.list());
	}

	private boolean nonLoaderResource(Resource resource) {
		return !resource.getPath().startsWith(this.loaderBasePath);
	}

	private List<Resource> asLoaderHidingResources(Collection<Resource> resources) {
		return resources.stream().filter(this::nonLoaderResource).map(this::asLoaderHidingResource).toList();
	}

	private Resource asLoaderHidingResource(Resource resource) {
		return (resource instanceof LoaderHidingResource) ? resource : new LoaderHidingResource(this.base, resource);
	}

	@Override
	public Resource resolve(String subUriPath) {
		if (subUriPath.startsWith(LOADER_RESOURCE_PATH_PREFIX)) {
			return null;
		}
		Resource resolved = this.delegate.resolve(subUriPath);
		return (resolved != null) ? new LoaderHidingResource(this.base, resolved) : null;
	}

	@Override
	public boolean isAlias() {
		return this.delegate.isAlias();
	}

	@Override
	public URI getRealURI() {
		return this.delegate.getRealURI();
	}

	@Override
	public void copyTo(Path destination) throws IOException {
		this.delegate.copyTo(destination);
	}

	@Override
	public Collection<Resource> getAllResources() {
		return asLoaderHidingResources(this.delegate.getAllResources());
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

}
