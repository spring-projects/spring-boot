/*
 * Copyright 2012-present the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.convert.DurationUnit;

/**
 * Cookie properties.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Weix Sun
 * @since 2.6.0
 */
public class Cookie {

	/**
	 * Name for the cookie.
	 */
	private @Nullable String name;

	/**
	 * Domain for the cookie.
	 */
	private @Nullable String domain;

	/**
	 * Path of the cookie.
	 */
	private @Nullable String path;

	/**
	 * Whether to use "HttpOnly" cookies for the cookie.
	 */
	private @Nullable Boolean httpOnly;

	/**
	 * Whether to always mark the cookie as secure.
	 */
	private @Nullable Boolean secure;

	/**
	 * Whether the generated cookie carries the Partitioned attribute.
	 */
	private @Nullable Boolean partitioned;

	/**
	 * Maximum age of the cookie. If a duration suffix is not specified, seconds will be
	 * used. A positive value indicates when the cookie expires relative to the current
	 * time. A value of 0 means the cookie should expire immediately. A negative value
	 * means no "Max-Age".
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private @Nullable Duration maxAge;

	/**
	 * SameSite setting for the cookie.
	 */
	private @Nullable SameSite sameSite;

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	public @Nullable String getDomain() {
		return this.domain;
	}

	public void setDomain(@Nullable String domain) {
		this.domain = domain;
	}

	public @Nullable String getPath() {
		return this.path;
	}

	public void setPath(@Nullable String path) {
		this.path = path;
	}

	public @Nullable Boolean getHttpOnly() {
		return this.httpOnly;
	}

	public void setHttpOnly(@Nullable Boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public @Nullable Boolean getSecure() {
		return this.secure;
	}

	public void setSecure(@Nullable Boolean secure) {
		this.secure = secure;
	}

	public @Nullable Duration getMaxAge() {
		return this.maxAge;
	}

	public void setMaxAge(@Nullable Duration maxAge) {
		this.maxAge = maxAge;
	}

	public @Nullable SameSite getSameSite() {
		return this.sameSite;
	}

	public void setSameSite(@Nullable SameSite sameSite) {
		this.sameSite = sameSite;
	}

	public @Nullable Boolean getPartitioned() {
		return this.partitioned;
	}

	public void setPartitioned(@Nullable Boolean partitioned) {
		this.partitioned = partitioned;
	}

	/**
	 * SameSite values.
	 */
	public enum SameSite {

		/**
		 * SameSite attribute will be omitted when creating the cookie.
		 */
		OMITTED(null),

		/**
		 * SameSite attribute will be set to None. Cookies are sent in both first-party
		 * and cross-origin requests.
		 */
		NONE("None"),

		/**
		 * SameSite attribute will be set to Lax. Cookies are sent in a first-party
		 * context, also when following a link to the origin site.
		 */
		LAX("Lax"),

		/**
		 * SameSite attribute will be set to Strict. Cookies are only sent in a
		 * first-party context (i.e. not when following a link to the origin site).
		 */
		STRICT("Strict");

		private @Nullable final String attributeValue;

		SameSite(@Nullable String attributeValue) {
			this.attributeValue = attributeValue;
		}

		public @Nullable String attributeValue() {
			return this.attributeValue;
		}

	}

}
