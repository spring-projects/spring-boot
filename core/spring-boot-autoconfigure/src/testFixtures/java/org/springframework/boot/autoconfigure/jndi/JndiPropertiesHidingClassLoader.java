/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.jndi;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Used as the thread context classloader to prevent {@code jndi.properties} resources
 * found on the classpath from triggering configuration of an InitialContextFactory.
 *
 * @author Andy Wilkinson
 */
public class JndiPropertiesHidingClassLoader extends ClassLoader {

	public JndiPropertiesHidingClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		if ("jndi.properties".equals(name)) {
			return Collections.emptyEnumeration();
		}
		return super.getResources(name);
	}

}
