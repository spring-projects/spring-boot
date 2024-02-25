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

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;

/**
 * {@link Origin} for an item loaded from a text resource. Provides access to the original
 * {@link Resource} that loaded the text and a {@link Location} within it. If the provided
 * resource provides an {@link Origin} (e.g. it is an {@link OriginTrackedResource}), then
 * it will be used as the {@link Origin#getParent() origin parent}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see OriginTrackedResource
 */
public class TextResourceOrigin implements Origin {

	private final Resource resource;

	private final Location location;

	/**
	 * Constructs a new TextResourceOrigin object with the specified resource and
	 * location.
	 * @param resource the resource associated with the origin
	 * @param location the location of the origin
	 */
	public TextResourceOrigin(Resource resource, Location location) {
		this.resource = resource;
		this.location = location;
	}

	/**
	 * Return the resource where the property originated.
	 * @return the text resource or {@code null}
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Return the location of the property within the source (if known).
	 * @return the location or {@code null}
	 */
	public Location getLocation() {
		return this.location;
	}

	/**
	 * Returns the parent origin of this TextResourceOrigin.
	 * @return the parent origin of this TextResourceOrigin
	 */
	@Override
	public Origin getParent() {
		return Origin.from(this.resource);
	}

	/**
	 * Compares this TextResourceOrigin object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof TextResourceOrigin other) {
			boolean result = true;
			result = result && ObjectUtils.nullSafeEquals(this.resource, other.resource);
			result = result && ObjectUtils.nullSafeEquals(this.location, other.location);
			return result;
		}
		return super.equals(obj);
	}

	/**
	 * Returns the hash code value for this TextResourceOrigin object. The hash code is
	 * generated based on the resource and location properties.
	 * @return the hash code value for this TextResourceOrigin object
	 */
	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.resource);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.location);
		return result;
	}

	/**
	 * Returns a string representation of the TextResourceOrigin object.
	 * @return a string representation of the TextResourceOrigin object
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getResourceDescription(this.resource));
		if (this.location != null) {
			result.append(" - ").append(this.location);
		}
		return result.toString();
	}

	/**
	 * Returns the description of the given resource. If the resource is an instance of
	 * OriginTrackedResource, the description of its underlying resource is returned. If
	 * the resource is null, the description "unknown resource [?]" is returned. If the
	 * resource is an instance of ClassPathResource, the description of the class path
	 * resource is returned. Otherwise, the description of the resource is returned.
	 * @param resource the resource for which the description is to be retrieved
	 * @return the description of the resource
	 */
	private String getResourceDescription(Resource resource) {
		if (resource instanceof OriginTrackedResource originTrackedResource) {
			return getResourceDescription(originTrackedResource.getResource());
		}
		if (resource == null) {
			return "unknown resource [?]";
		}
		if (resource instanceof ClassPathResource classPathResource) {
			return getResourceDescription(classPathResource);
		}
		return resource.getDescription();
	}

	/**
	 * Retrieves the description of a resource.
	 * @param resource The ClassPathResource to retrieve the description from.
	 * @return The description of the resource, or the resource's description if it is not
	 * a JarUri.
	 */
	private String getResourceDescription(ClassPathResource resource) {
		try {
			JarUri jarUri = JarUri.from(resource.getURI());
			if (jarUri != null) {
				return jarUri.getDescription(resource.getDescription());
			}
		}
		catch (IOException ex) {
			// Ignore
		}
		return resource.getDescription();
	}

	/**
	 * A location (line and column number) within the resource.
	 */
	public static final class Location {

		private final int line;

		private final int column;

		/**
		 * Create a new {@link Location} instance.
		 * @param line the line number (zero indexed)
		 * @param column the column number (zero indexed)
		 */
		public Location(int line, int column) {
			this.line = line;
			this.column = column;
		}

		/**
		 * Return the line of the text resource where the property originated.
		 * @return the line number (zero indexed)
		 */
		public int getLine() {
			return this.line;
		}

		/**
		 * Return the column of the text resource where the property originated.
		 * @return the column number (zero indexed)
		 */
		public int getColumn() {
			return this.column;
		}

		/**
		 * Compares this Location object to the specified object for equality.
		 * @param obj the object to compare to
		 * @return true if the specified object is equal to this Location object, false
		 * otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Location other = (Location) obj;
			boolean result = true;
			result = result && this.line == other.line;
			result = result && this.column == other.column;
			return result;
		}

		/**
		 * Returns a hash code value for the object. The hash code is calculated by
		 * multiplying the line number by 31 and adding the column number.
		 * @return the hash code value for the object
		 */
		@Override
		public int hashCode() {
			return (31 * this.line) + this.column;
		}

		/**
		 * Returns a string representation of the Location object. The string is formatted
		 * as "line:column", where line and column are the respective values incremented
		 * by 1.
		 * @return a string representation of the Location object
		 */
		@Override
		public String toString() {
			return (this.line + 1) + ":" + (this.column + 1);
		}

	}

}
