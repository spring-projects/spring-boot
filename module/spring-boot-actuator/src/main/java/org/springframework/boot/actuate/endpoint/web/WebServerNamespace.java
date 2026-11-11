/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * A web server namespace used for disambiguation when multiple web servers are running in
 * the same application (for example a management context running on a different port).
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.6.0
 */
public final class WebServerNamespace {

	private static final String WEB_SERVER_CONTEXT_CLASS = "org.springframework.boot.web.server.context.WebServerApplicationContext";

	/**
	 * {@link WebServerNamespace} that represents the main server.
	 */
	public static final WebServerNamespace SERVER = new WebServerNamespace("server");

	/**
	 * {@link WebServerNamespace} that represents the management server.
	 */
	public static final WebServerNamespace MANAGEMENT = new WebServerNamespace("management");

	private final String value;

	private WebServerNamespace(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the namespace.
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		WebServerNamespace other = (WebServerNamespace) obj;
		return this.value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a new {@link WebServerNamespace} from a value. If the
	 * context is {@code null} or not a web server context then {@link #SERVER} is
	 * returned.
	 * @param context the application context
	 * @return the web server namespace
	 * @since 4.0.1
	 */
	public static WebServerNamespace from(@Nullable ApplicationContext context) {
		if (!ClassUtils.isPresent(WEB_SERVER_CONTEXT_CLASS, null)) {
			return SERVER;
		}
		return from(WebServerApplicationContext.getServerNamespace(context));
	}

	/**
	 * Factory method to create a new {@link WebServerNamespace} from a value. If the
	 * value is empty or {@code null} then {@link #SERVER} is returned.
	 * @param value the namespace value or {@code null}
	 * @return the web server namespace
	 */
	public static WebServerNamespace from(@Nullable String value) {
		if (StringUtils.hasText(value)) {
			return new WebServerNamespace(value);
		}
		return SERVER;
	}

}
