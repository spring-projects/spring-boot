/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A predicate for a request to an operation on a web endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class WebOperationRequestPredicate {

	private static final Pattern PATH_VAR_PATTERN = Pattern.compile("(\\{\\*?).+?}");

	private static final Pattern ALL_REMAINING_PATH_SEGMENTS_VAR_PATTERN = Pattern.compile("^.*\\{\\*(.+?)}$");

	private final String path;

	private final String matchAllRemainingPathSegmentsVariable;

	private final String canonicalPath;

	private final WebEndpointHttpMethod httpMethod;

	private final Collection<String> consumes;

	private final Collection<String> produces;

	/**
	 * Creates a new {@code OperationRequestPredicate}.
	 * @param path the path for the operation
	 * @param httpMethod the HTTP method that the operation supports
	 * @param produces the media types that the operation produces
	 * @param consumes the media types that the operation consumes
	 */
	public WebOperationRequestPredicate(String path, WebEndpointHttpMethod httpMethod, Collection<String> consumes,
			Collection<String> produces) {
		this.path = path;
		this.canonicalPath = extractCanonicalPath(path);
		this.matchAllRemainingPathSegmentsVariable = extractMatchAllRemainingPathSegmentsVariable(path);
		this.httpMethod = httpMethod;
		this.consumes = consumes;
		this.produces = produces;
	}

	/**
     * Extracts the canonical path from the given path by replacing any path variables with a wildcard character.
     * 
     * @param path the path to extract the canonical path from
     * @return the canonical path with path variables replaced by a wildcard character
     */
    private String extractCanonicalPath(String path) {
		Matcher matcher = PATH_VAR_PATTERN.matcher(path);
		return matcher.replaceAll("$1*}");
	}

	/**
     * Extracts the value of the match all remaining path segments variable from the given path.
     * 
     * @param path the path to extract the variable from
     * @return the value of the match all remaining path segments variable, or null if not found
     */
    private String extractMatchAllRemainingPathSegmentsVariable(String path) {
		Matcher matcher = ALL_REMAINING_PATH_SEGMENTS_VAR_PATTERN.matcher(path);
		return matcher.matches() ? matcher.group(1) : null;
	}

	/**
	 * Returns the path for the operation.
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Returns the name of the variable used to catch all remaining path segments
	 * {@code null}.
	 * @return the variable name
	 * @since 2.2.0
	 */
	public String getMatchAllRemainingPathSegmentsVariable() {
		return this.matchAllRemainingPathSegmentsVariable;
	}

	/**
	 * Returns the HTTP method for the operation.
	 * @return the HTTP method
	 */
	public WebEndpointHttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Returns the media types that the operation consumes.
	 * @return the consumed media types
	 */
	public Collection<String> getConsumes() {
		return Collections.unmodifiableCollection(this.consumes);
	}

	/**
	 * Returns the media types that the operation produces.
	 * @return the produced media types
	 */
	public Collection<String> getProduces() {
		return Collections.unmodifiableCollection(this.produces);
	}

	/**
     * Compares this WebOperationRequestPredicate to the specified object for equality.
     * Returns true if the specified object is also a WebOperationRequestPredicate and all the fields are equal.
     * 
     * @param obj the object to compare with
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
		WebOperationRequestPredicate other = (WebOperationRequestPredicate) obj;
		boolean result = true;
		result = result && this.consumes.equals(other.consumes);
		result = result && this.httpMethod == other.httpMethod;
		result = result && this.canonicalPath.equals(other.canonicalPath);
		result = result && this.produces.equals(other.produces);
		return result;
	}

	/**
     * Returns the hash code value for this WebOperationRequestPredicate object.
     * The hash code is generated based on the consumes, httpMethod, canonicalPath, and produces properties.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.consumes.hashCode();
		result = prime * result + this.httpMethod.hashCode();
		result = prime * result + this.canonicalPath.hashCode();
		result = prime * result + this.produces.hashCode();
		return result;
	}

	/**
     * Returns a string representation of the WebOperationRequestPredicate object.
     * 
     * @return the string representation of the WebOperationRequestPredicate object
     */
    @Override
	public String toString() {
		StringBuilder result = new StringBuilder(this.httpMethod + " to path '" + this.path + "'");
		if (!CollectionUtils.isEmpty(this.consumes)) {
			result.append(" consumes: ").append(StringUtils.collectionToCommaDelimitedString(this.consumes));
		}
		if (!CollectionUtils.isEmpty(this.produces)) {
			result.append(" produces: ").append(StringUtils.collectionToCommaDelimitedString(this.produces));
		}
		return result.toString();
	}

}
