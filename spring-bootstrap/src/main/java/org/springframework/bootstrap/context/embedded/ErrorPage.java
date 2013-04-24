/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.bootstrap.context.embedded;

import org.springframework.util.ObjectUtils;

/**
 * Simple container-independent abstraction for servlet error pages. Roughly equivalent to
 * the {@literal &lt;error-page&gt;} element traditionally found in web.xml.
 * 
 * @author Dave Syer
 * @since 4.0
 */
public class ErrorPage {

	private String path;

	private Class<? extends Throwable> exception = null;

	private int status = 0;

	public ErrorPage(String path) {
		super();
		this.path = path;
	}

	public ErrorPage(int status, String path) {
		super();
		this.status = status;
		this.path = path;
	}

	public ErrorPage(Class<? extends Throwable> exception, String path) {
		super();
		this.exception = exception;
		this.path = path;
	}

	/**
	 * The path to render (usually implemented as a forward), starting with "/". A custom
	 * controller or servlet path can be used, or if the container supports it, a template
	 * path (e.g. "/error.jsp").
	 * 
	 * @return the path that will be rendered for this error
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * @return the exception type (or null for a page that matches by status)
	 */
	public Class<? extends Throwable> getException() {
		return this.exception;
	}

	/**
	 * The HTTP status value that this error page matches.
	 * 
	 * @return the status value (or 0 for a page that matches any status)
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * The exception type name.
	 * 
	 * @return the exception type name (or null if there is none)
	 */
	public String getExceptionName() {
		return this.exception == null ? null : this.exception.getName();
	}

	/**
	 * @return is this error page a global one (matches all unmatched status and exception
	 * types)?
	 */
	public boolean isGlobal() {
		return this.status == 0 && this.exception == null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(getExceptionName());
		result = prime * result + ObjectUtils.nullSafeHashCode(this.path);
		result = prime * result + this.status;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ErrorPage other = (ErrorPage) obj;
		boolean rtn = true;
		rtn &= ObjectUtils.nullSafeEquals(getExceptionName(), other.getExceptionName());
		rtn &= ObjectUtils.nullSafeEquals(this.path, other.path);
		rtn &= this.status == other.status;
		return rtn;
	}

}
