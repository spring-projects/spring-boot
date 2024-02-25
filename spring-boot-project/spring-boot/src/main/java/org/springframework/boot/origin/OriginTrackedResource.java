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

	/**
     * Returns an input stream for reading the contents of this OriginTrackedResource.
     * 
     * @return an input stream for reading the contents of this OriginTrackedResource
     * @throws IOException if an I/O error occurs while opening the input stream
     */
    @Override
	public InputStream getInputStream() throws IOException {
		return getResource().getInputStream();
	}

	/**
     * Returns a boolean value indicating whether the resource exists.
     * 
     * @return {@code true} if the resource exists, {@code false} otherwise.
     */
    @Override
	public boolean exists() {
		return getResource().exists();
	}

	/**
     * Returns a boolean value indicating whether the resource is readable.
     * 
     * @return true if the resource is readable, false otherwise
     */
    @Override
	public boolean isReadable() {
		return getResource().isReadable();
	}

	/**
     * Returns a boolean value indicating whether the resource is open.
     * 
     * @return {@code true} if the resource is open, {@code false} otherwise.
     */
    @Override
	public boolean isOpen() {
		return getResource().isOpen();
	}

	/**
     * Returns a boolean value indicating whether the resource is a file.
     * 
     * @return true if the resource is a file, false otherwise
     */
    @Override
	public boolean isFile() {
		return getResource().isFile();
	}

	/**
     * Returns the URL of the resource.
     * 
     * @return the URL of the resource
     * @throws IOException if an I/O error occurs while getting the URL
     */
    @Override
	public URL getURL() throws IOException {
		return getResource().getURL();
	}

	/**
     * Returns the URI of the resource.
     *
     * @return the URI of the resource
     * @throws IOException if an I/O error occurs
     */
    @Override
	public URI getURI() throws IOException {
		return getResource().getURI();
	}

	/**
     * Returns the file associated with this OriginTrackedResource.
     * 
     * @return the file associated with this OriginTrackedResource
     * @throws IOException if an I/O error occurs
     */
    @Override
	public File getFile() throws IOException {
		return getResource().getFile();
	}

	/**
     * Returns a readable byte channel for this OriginTrackedResource.
     *
     * @return the readable byte channel for this OriginTrackedResource
     * @throws IOException if an I/O error occurs
     */
    @Override
	public ReadableByteChannel readableChannel() throws IOException {
		return getResource().readableChannel();
	}

	/**
     * Returns the content length of the resource.
     *
     * @return the content length of the resource
     * @throws IOException if an I/O error occurs while retrieving the content length
     */
    @Override
	public long contentLength() throws IOException {
		return getResource().contentLength();
	}

	/**
     * Returns the last modified timestamp of the resource.
     *
     * @return the last modified timestamp of the resource
     * @throws IOException if an I/O error occurs while retrieving the last modified timestamp
     */
    @Override
	public long lastModified() throws IOException {
		return getResource().lastModified();
	}

	/**
     * Creates a new resource by appending the specified relative path to the current resource's path.
     * 
     * @param relativePath the relative path to append
     * @return the newly created resource
     * @throws IOException if an I/O error occurs while creating the resource
     */
    @Override
	public Resource createRelative(String relativePath) throws IOException {
		return getResource().createRelative(relativePath);
	}

	/**
     * Returns the filename of the resource.
     * 
     * @return the filename of the resource
     */
    @Override
	public String getFilename() {
		return getResource().getFilename();
	}

	/**
     * Returns the description of the resource.
     * 
     * @return the description of the resource
     */
    @Override
	public String getDescription() {
		return getResource().getDescription();
	}

	/**
     * Returns the resource associated with this OriginTrackedResource.
     *
     * @return the resource associated with this OriginTrackedResource
     */
    public Resource getResource() {
		return this.resource;
	}

	/**
     * Returns the origin of the OriginTrackedResource.
     *
     * @return the origin of the OriginTrackedResource
     */
    @Override
	public Origin getOrigin() {
		return this.origin;
	}

	/**
     * Compares this OriginTrackedResource with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the specified object is equal to this OriginTrackedResource, false otherwise
     */
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

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the {@code hashCode()} method.
     * The hash code is calculated based on the hash code of the resource and the origin.
     * 
     * @return the hash code value for the object
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = this.resource.hashCode();
		result = prime * result + ObjectUtils.nullSafeHashCode(this.origin);
		return result;
	}

	/**
     * Returns a string representation of the OriginTrackedResource object.
     * 
     * @return a string representation of the OriginTrackedResource object
     */
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

		/**
         * Returns the resource as a writable resource.
         * 
         * @return the resource as a writable resource
         */
        @Override
		public WritableResource getResource() {
			return (WritableResource) super.getResource();
		}

		/**
         * Returns the output stream for writing to the resource.
         *
         * @return the output stream for writing to the resource
         * @throws IOException if an I/O error occurs
         */
        @Override
		public OutputStream getOutputStream() throws IOException {
			return getResource().getOutputStream();
		}

	}

}
