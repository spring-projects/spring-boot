/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools;

/**
 * The scope of a library. The common {@link #COMPILE}, {@link #RUNTIME} and
 * {@link #PROVIDED} scopes are defined here and supported by the common {@link Layouts}.
 * A custom {@link Layout} can handle additional scopes as required.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public interface LibraryScope {

	@Override
	String toString();

	/**
	 * The library is used at compile time and runtime.
	 */
	LibraryScope COMPILE = new LibraryScope() {

		@Override
		public String toString() {
			return "compile";
		}

	};

	/**
	 * The library is used at runtime but not needed for compile.
	 */
	LibraryScope RUNTIME = new LibraryScope() {

		@Override
		public String toString() {
			return "runtime";
		}

	};

	/**
	 * The library is needed for compile but is usually provided when running.
	 */
	LibraryScope PROVIDED = new LibraryScope() {

		@Override
		public String toString() {
			return "provided";
		}

	};

	/**
	 * Marker for custom scope when custom configuration is used.
	 */
	LibraryScope CUSTOM = new LibraryScope() {

		@Override
		public String toString() {
			return "custom";
		}

	};

}
