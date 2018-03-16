/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A predicate for a request to an operation on a web endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class WebOperationRequestPredicate {

	private final String path;

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
	public WebOperationRequestPredicate(String path, WebEndpointHttpMethod httpMethod,
			Collection<String> consumes, Collection<String> produces) {
		this.path = path;
		this.canonicalPath = path.replaceAll("\\{.*?}", "{*}");
		this.httpMethod = httpMethod;
		this.consumes = consumes;
		this.produces = produces;
	}

	/**
	 * Returns the path for the operation.
	 * @return the path
	 */
	public String getPath() {
		return this.path;
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

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(
				this.httpMethod + " to path '" + this.path + "'");
		if (!CollectionUtils.isEmpty(this.consumes)) {
			result.append(" consumes: "
					+ StringUtils.collectionToCommaDelimitedString(this.consumes));
		}
		if (!CollectionUtils.isEmpty(this.produces)) {
			result.append(" produces: "
					+ StringUtils.collectionToCommaDelimitedString(this.produces));
		}
		return result.toString();
	}

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

}
