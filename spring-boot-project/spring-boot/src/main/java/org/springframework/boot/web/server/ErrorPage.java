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

package org.springframework.boot.web.server;

import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;

/**
 * Simple server-independent abstraction for error pages. Roughly equivalent to the
 * {@literal &lt;error-page&gt;} element traditionally found in web.xml.
 *
 * @author Dave Syer
 * @since 2.0.0
 */
public class ErrorPage {

	private final HttpStatus status;

	private final Class<? extends Throwable> exception;

	private final String path;

	/**
     * Constructs a new ErrorPage object with the specified path.
     * 
     * @param path the path of the error page
     */
    public ErrorPage(String path) {
		this.status = null;
		this.exception = null;
		this.path = path;
	}

	/**
     * Constructs a new ErrorPage with the specified HttpStatus and path.
     *
     * @param status the HttpStatus representing the error status
     * @param path the path of the error page
     */
    public ErrorPage(HttpStatus status, String path) {
		this.status = status;
		this.exception = null;
		this.path = path;
	}

	/**
     * Constructs a new ErrorPage with the specified exception and path.
     * 
     * @param exception the class of the exception to be handled by this error page
     * @param path the path to the error page
     */
    public ErrorPage(Class<? extends Throwable> exception, String path) {
		this.status = null;
		this.exception = exception;
		this.path = path;
	}

	/**
	 * The path to render (usually implemented as a forward), starting with "/". A custom
	 * controller or servlet path can be used, or if the server supports it, a template
	 * path (e.g. "/error.jsp").
	 * @return the path that will be rendered for this error
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Returns the exception type (or {@code null} for a page that matches by status).
	 * @return the exception type or {@code null}
	 */
	public Class<? extends Throwable> getException() {
		return this.exception;
	}

	/**
	 * The HTTP status value that this error page matches (or {@code null} for a page that
	 * matches by exception).
	 * @return the status or {@code null}
	 */
	public HttpStatus getStatus() {
		return this.status;
	}

	/**
	 * The HTTP status value that this error page matches.
	 * @return the status value (or 0 for a page that matches any status)
	 */
	public int getStatusCode() {
		return (this.status != null) ? this.status.value() : 0;
	}

	/**
	 * The exception type name.
	 * @return the exception type name (or {@code null} if there is none)
	 */
	public String getExceptionName() {
		return (this.exception != null) ? this.exception.getName() : null;
	}

	/**
	 * Return if this error page is a global one (matches all unmatched status and
	 * exception types).
	 * @return if this is a global error page
	 */
	public boolean isGlobal() {
		return (this.status == null && this.exception == null);
	}

	/**
     * Compares this ErrorPage object to the specified object for equality.
     * 
     * @param obj the object to compare to
     * @return true if the specified object is equal to this ErrorPage object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof ErrorPage other) {
			return ObjectUtils.nullSafeEquals(getExceptionName(), other.getExceptionName())
					&& ObjectUtils.nullSafeEquals(this.path, other.path) && this.status == other.status;
		}
		return false;
	}

	/**
     * Returns a hash code value for the ErrorPage object.
     * 
     * The hash code is calculated based on the exception name, path, and status code of the ErrorPage.
     * 
     * @return the hash code value for the ErrorPage object
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(getExceptionName());
		result = prime * result + ObjectUtils.nullSafeHashCode(this.path);
		result = prime * result + getStatusCode();
		return result;
	}

}
