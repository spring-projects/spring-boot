/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.origin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Decorator that can be used to add {@link Origin} information to a {@link Resource} or
 * {@link WritableResource}.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see #of(Resource, Origin)
 * @see #of(WritableResource, Origin)
 * @see OriginProvider
 */
public class OriginTrackedResource implements Resource, OriginProvider {

	private final Resource resource;

	private final Origin origin;

	/**
	 * Create a new {@link OriginTrackedResource} instance.
	 * @param resource the resource to track
	 * @param origin the origin of the resource
	 */
	OriginTrackedResource(Resource resource, Origin origin) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.origin = origin;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return getResource().getInputStream();
	}

	@Override
	public boolean exists() {
		return getResource().exists();
	}

	@Override
	public boolean isReadable() {
		return getResource().isReadable();
	}

	@Override
	public boolean isOpen() {
		return getResource().isOpen();
	}

	@Override
	public boolean isFile() {
		return getResource().isFile();
	}

	@Override
	public URL getURL() throws IOException {
		return getResource().getURL();
	}

	@Override
	public URI getURI() throws IOException {
		return getResource().getURI();
	}

	@Override
	public File getFile() throws IOException {
		return getResource().getFile();
	}

	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return getResource().readableChannel();
	}

	@Override
	public long contentLength() throws IOException {
		return getResource().contentLength();
	}

	@Override
	public long lastModified() throws IOException {
		return getResource().lastModified();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return getResource().createRelative(relativePath);
	}

	@Override
	public String getFilename() {
		return getResource().getFilename();
	}

	@Override
	public String getDescription() {
		return getResource().getDescription();
	}

	public Resource getResource() {
		return this.resource;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		OriginTrackedResource other = (OriginTrackedResource) obj;
		return this.resource.equals(other) && ObjectUtils.nullSafeEquals(this.origin, other.origin);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = this.resource.hashCode();
		result = prime * result + ObjectUtils.nullSafeHashCode(this.origin);
		return result;
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}

	/**
	 * Return a new {@link OriginProvider origin tracked} version the given
	 * {@link WritableResource}.
	 * @param resource the tracked resource
	 * @param origin the origin of the resource
	 * @return an {@link OriginTrackedWritableResource} instance
	 */
	public static OriginTrackedWritableResource of(WritableResource resource, Origin origin) {
		return (OriginTrackedWritableResource) of((Resource) resource, origin);
	}

	/**
	 * Return a new {@link OriginProvider origin tracked} version the given
	 * {@link Resource}.
	 * @param resource the tracked resource
	 * @param origin the origin of the resource
	 * @return an {@link OriginTrackedResource} instance
	 */
	public static OriginTrackedResource of(Resource resource, Origin origin) {
		if (resource instanceof WritableResource writableResource) {
			return new OriginTrackedWritableResource(writableResource, origin);
		}
		return new OriginTrackedResource(resource, origin);
	}

	/**
	 * Variant of {@link OriginTrackedResource} for {@link WritableResource} instances.
	 */
	public static class OriginTrackedWritableResource extends OriginTrackedResource implements WritableResource {

		/**
		 * Create a new {@link OriginTrackedWritableResource} instance.
		 * @param resource the resource to track
		 * @param origin the origin of the resource
		 */
		OriginTrackedWritableResource(WritableResource resource, Origin origin) {
			super(resource, origin);
		}

		@Override
		public WritableResource getResource() {
			return (WritableResource) super.getResource();
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return getResource().getOutputStream();
		}

	}

}
