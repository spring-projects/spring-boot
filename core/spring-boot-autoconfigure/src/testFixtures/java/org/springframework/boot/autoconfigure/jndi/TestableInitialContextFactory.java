/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * An {@code InitialContextFactory} implementation to be used for testing JNDI.
 *
 * @author Stephane Nicoll
 */
public class TestableInitialContextFactory implements InitialContextFactory {

	private static TestableContext context;

	@Override
	public Context getInitialContext(Hashtable<?, ?> environment) {
		return getContext();
	}

	public static void bind(String name, Object obj) {
		try {
			getContext().bind(name, obj);
		}
		catch (NamingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static void clearAll() {
		getContext().clearAll();
	}

	private static TestableContext getContext() {
		if (context == null) {
			try {
				context = new TestableContext();
			}
			catch (NamingException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return context;
	}

	private static final class TestableContext extends InitialContext {

		private final Map<String, Object> bindings = new HashMap<>();

		private TestableContext() throws NamingException {
			super(true);
		}

		@Override
		public void bind(String name, Object obj) throws NamingException {
			this.bindings.put(name, obj);
		}

		@Override
		public Object lookup(String name) {
			return this.bindings.get(name);
		}

		@Override
		public Hashtable<?, ?> getEnvironment() throws NamingException {
			return new Hashtable<>(); // Used to detect if JNDI is
										// available
		}

		void clearAll() {
			this.bindings.clear();
		}

	}

}
