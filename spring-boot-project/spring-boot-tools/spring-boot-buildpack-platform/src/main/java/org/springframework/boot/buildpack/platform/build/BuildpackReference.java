/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.util.Assert;

/**
 * An opaque reference to a {@link Buildpack}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.5.0
 * @see BuildpackResolver
 */
public final class BuildpackReference {

	private final String value;

	/**
	 * Constructs a new BuildpackReference with the specified value.
	 * @param value the value of the BuildpackReference
	 */
	private BuildpackReference(String value) {
		this.value = value;
	}

	/**
	 * Checks if the value of the BuildpackReference object has the specified prefix.
	 * @param prefix the prefix to check
	 * @return true if the value starts with the specified prefix, false otherwise
	 */
	boolean hasPrefix(String prefix) {
		return this.value.startsWith(prefix);
	}

	/**
	 * Returns a sub-reference of the current value by removing the specified prefix.
	 * @param prefix the prefix to be removed from the value
	 * @return the sub-reference of the value after removing the prefix, or null if the
	 * value does not start with the prefix
	 */
	String getSubReference(String prefix) {
		return this.value.startsWith(prefix) ? this.value.substring(prefix.length()) : null;
	}

	/**
	 * Returns a Path object representing the file path of the BuildpackReference.
	 * @return the Path object representing the file path, or null if the path is not a
	 * valid file path
	 * @throws MalformedURLException if the path is a malformed URL
	 */
	Path asPath() {
		try {
			URL url = new URL(this.value);
			if (url.getProtocol().equals("file")) {
				return Paths.get(url.getPath());
			}
			return null;
		}
		catch (MalformedURLException ex) {
			// not a URL, fall through to attempting to find a plain file path
		}
		try {
			return Paths.get(this.value);
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Compares this BuildpackReference object to the specified object for equality.
	 * @param obj the object to compare with
	 * @return true if the specified object is equal to this BuildpackReference object,
	 * false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((BuildpackReference) obj).value);
	}

	/**
	 * Returns the hash code value for this BuildpackReference object.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	/**
	 * Returns a string representation of the object.
	 * @return the string representation of the object
	 */
	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Create a new {@link BuildpackReference} from the given value.
	 * @param value the value to use
	 * @return a new {@link BuildpackReference}
	 */
	public static BuildpackReference of(String value) {
		Assert.hasText(value, "Value must not be empty");
		return new BuildpackReference(value);
	}

}
