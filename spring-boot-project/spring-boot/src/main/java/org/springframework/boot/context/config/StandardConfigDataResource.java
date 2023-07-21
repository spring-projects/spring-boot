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

	boolean isEmptyDirectory() {
		return this.emptyDirectory;
	}

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

	private boolean isSameUnderlyingResource(Resource ours, Resource other) {
		return ours.equals(other) || isSameFile(getUnderlyingFile(ours), getUnderlyingFile(other));
	}

	private boolean isSameFile(File ours, File other) {
		return (ours != null) && (other != null) && ours.equals(other);
	}

	@Override
	public int hashCode() {
		File underlyingFile = getUnderlyingFile(this.resource);
		return (underlyingFile != null) ? underlyingFile.hashCode() : this.resource.hashCode();
	}

	@Override
	public String toString() {
		if (this.resource instanceof FileSystemResource || this.resource instanceof FileUrlResource) {
			try {
				return "file [" + this.resource.getFile().toString() + "]";
			}
			catch (IOException ex) {
			}
		}
		return this.resource.toString();
	}

	private File getUnderlyingFile(Resource resource) {
		try {
			if (resource instanceof ClassPathResource || resource instanceof FileSystemResource
					|| resource instanceof FileUrlResource) {
				File file = resource.getFile();
				return (file != null) ? file.getAbsoluteFile() : null;
			}
		}
		catch (IOException ex) {
		}
		return null;
	}

}
