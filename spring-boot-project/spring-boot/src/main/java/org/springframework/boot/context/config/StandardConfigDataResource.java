/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataResource} backed by a {@link Resource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.4.0
 */
public class StandardConfigDataResource extends ConfigDataResource {

	private final StandardConfigDataReference reference;

	private final Resource resource;

	private final boolean emptyDirectory;

	/**
	 * Create a new {@link StandardConfigDataResource} instance.
	 * @param reference the resource reference
	 * @param resource the underlying resource
	 */
	StandardConfigDataResource(StandardConfigDataReference reference, Resource resource) {
		this(reference, resource, false);
	}

	/**
	 * Create a new {@link StandardConfigDataResource} instance.
	 * @param reference the resource reference
	 * @param resource the underlying resource
	 * @param emptyDirectory if the resource is an empty directory that we know exists
	 */
	StandardConfigDataResource(StandardConfigDataReference reference, Resource resource, boolean emptyDirectory) {
		Assert.notNull(reference, "Reference must not be null");
		Assert.notNull(resource, "Resource must not be null");
		this.reference = reference;
		this.resource = resource;
		this.emptyDirectory = emptyDirectory;
	}

	/**
	 * Returns the reference to the StandardConfigDataReference object.
	 * @return the reference to the StandardConfigDataReference object
	 */
	StandardConfigDataReference getReference() {
		return this.reference;
	}

	/**
	 * Return the underlying Spring {@link Resource} being loaded.
	 * @return the underlying resource
	 * @since 2.4.2
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Return the profile or {@code null} if the resource is not profile specific.
	 * @return the profile or {@code null}
	 * @since 2.4.6
	 */
	public String getProfile() {
		return this.reference.getProfile();
	}

	/**
	 * Returns a boolean value indicating whether the directory is empty or not.
	 * @return true if the directory is empty, false otherwise
	 */
	boolean isEmptyDirectory() {
		return this.emptyDirectory;
	}

	/**
	 * Compares this StandardConfigDataResource object to the specified object for
	 * equality.
	 * @param obj the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		StandardConfigDataResource other = (StandardConfigDataResource) obj;
		return (this.emptyDirectory == other.emptyDirectory) && isSameUnderlyingResource(this.resource, other.resource);
	}

	/**
	 * Checks if two resources have the same underlying file or are equal.
	 * @param ours the first resource to compare
	 * @param other the second resource to compare
	 * @return true if the resources have the same underlying file or are equal, false
	 * otherwise
	 */
	private boolean isSameUnderlyingResource(Resource ours, Resource other) {
		return ours.equals(other) || isSameFile(getUnderlyingFile(ours), getUnderlyingFile(other));
	}

	/**
	 * Checks if two files are the same.
	 * @param ours the first file to compare
	 * @param other the second file to compare
	 * @return true if the files are the same, false otherwise
	 */
	private boolean isSameFile(File ours, File other) {
		return (ours != null) && ours.equals(other);
	}

	/**
	 * Returns the hash code value for this StandardConfigDataResource object.
	 *
	 * The hash code is calculated based on the underlying file associated with the
	 * resource, if available. If the underlying file is not available, the hash code is
	 * calculated based on the resource itself.
	 * @return the hash code value for this StandardConfigDataResource object
	 */
	@Override
	public int hashCode() {
		File underlyingFile = getUnderlyingFile(this.resource);
		return (underlyingFile != null) ? underlyingFile.hashCode() : this.resource.hashCode();
	}

	/**
	 * Returns a string representation of the object. If the resource is an instance of
	 * FileSystemResource or FileUrlResource, it returns the file path of the resource. If
	 * an IOException occurs while getting the file path, it ignores the exception.
	 * Otherwise, it returns the string representation of the resource.
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		if (this.resource instanceof FileSystemResource || this.resource instanceof FileUrlResource) {
			try {
				return "file [" + this.resource.getFile() + "]";
			}
			catch (IOException ex) {
				// Ignore
			}
		}
		return this.resource.toString();
	}

	/**
	 * Returns the underlying file for the given resource.
	 * @param resource the resource for which to retrieve the underlying file
	 * @return the underlying file for the resource, or null if it cannot be obtained
	 */
	private File getUnderlyingFile(Resource resource) {
		try {
			if (resource instanceof ClassPathResource || resource instanceof FileSystemResource
					|| resource instanceof FileUrlResource) {
				return resource.getFile().getAbsoluteFile();
			}
		}
		catch (IOException ex) {
			// Ignore
		}
		return null;
	}

}
