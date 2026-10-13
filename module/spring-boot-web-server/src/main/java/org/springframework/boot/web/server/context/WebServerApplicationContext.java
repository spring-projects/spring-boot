/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.ObjectUtils;

/**
 * Interface to be implemented by {@link ApplicationContext application contexts} that
 * create and manage the lifecycle of an embedded {@link WebServer}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface WebServerApplicationContext extends ApplicationContext {

	/**
	 * {@link SmartLifecycle#getPhase() SmartLifecycle phase} in which graceful shutdown
	 * of the web server is performed.
	 * @since 4.0.0
	 */
	int GRACEFUL_SHUTDOWN_PHASE = SmartLifecycle.DEFAULT_PHASE - 1024;

	/**
	 * {@link SmartLifecycle#getPhase() SmartLifecycle phase} in which starting and
	 * stopping of the web server is performed.
	 */
	int START_STOP_LIFECYCLE_PHASE = GRACEFUL_SHUTDOWN_PHASE - 1024;

	/**
	 * Returns the {@link WebServer} that was created by the context or {@code null} if
	 * the server has not yet been created.
	 * @return the web server
	 */
	@Nullable WebServer getWebServer();

	/**
	 * Returns the namespace of the web server application context or {@code null} if no
	 * namespace has been set. Used for disambiguation when multiple web servers are
	 * running in the same application (for example a management context running on a
	 * different port).
	 * @return the server namespace
	 */
	@Nullable String getServerNamespace();

	/**
	 * Returns {@code true} if the specified context is a
	 * {@link WebServerApplicationContext} with a matching server namespace.
	 * @param context the context to check
	 * @param serverNamespace the server namespace to match against
	 * @return {@code true} if the server namespace of the context matches
	 */
	static boolean hasServerNamespace(@Nullable ApplicationContext context, String serverNamespace) {
		return (context instanceof WebServerApplicationContext webServerApplicationContext)
				&& ObjectUtils.nullSafeEquals(webServerApplicationContext.getServerNamespace(), serverNamespace);
	}

	/**
	 * Returns the server namespace if the specified context is a
	 * {@link WebServerApplicationContext}.
	 * @param context the context
	 * @return the server namespace or {@code null} if the context is not a
	 * {@link WebServerApplicationContext}
	 */
	static @Nullable String getServerNamespace(@Nullable ApplicationContext context) {
		return (context instanceof WebServerApplicationContext configurableContext)
				? configurableContext.getServerNamespace() : null;

	}

}
