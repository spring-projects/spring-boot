/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.tools;

/**
 * The scope of a library. The common {@link #COMPILE}, {@link #RUNTIME} and
 * {@link #PROVIDED} scopes are defined here and supported by the common {@link Layouts}.
 * A custom {@link Layout} can handle additional scopes as required.
 *
 * @author Phillip Webb
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
