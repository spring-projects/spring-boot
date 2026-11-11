/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Utility used to register loader {@link URLStreamHandler URL handlers}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public final class Handlers {

	private static final String PROTOCOL_HANDLER_PACKAGES = "java.protocol.handler.pkgs";

	private static final String PACKAGE = Handlers.class.getPackageName();

	private Handlers() {
	}

	/**
	 * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
	 * {@link URLStreamHandler} will be located to deal with jar URLs.
	 */
	public static void register() {
		String packages = System.getProperty(PROTOCOL_HANDLER_PACKAGES, "");
		packages = (!packages.isEmpty() && !packages.contains(PACKAGE)) ? packages + "|" + PACKAGE : PACKAGE;
		System.setProperty(PROTOCOL_HANDLER_PACKAGES, packages);
		resetCachedUrlHandlers();
	}

	/**
	 * Reset any cached handlers just in case a jar protocol has already been used. We
	 * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
	 * should have no effect other than clearing the handlers cache.
	 */
	private static void resetCachedUrlHandlers() {
		try {
			URL.setURLStreamHandlerFactory(null);
		}
		catch (Error ex) {
			// Ignore
		}
	}

}
