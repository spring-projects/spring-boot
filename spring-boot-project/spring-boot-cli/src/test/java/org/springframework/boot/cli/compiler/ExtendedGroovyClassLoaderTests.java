/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ExtendedGroovyClassLoader}.
 *
 * @author Phillip Webb
 */
class ExtendedGroovyClassLoaderTests {

	private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

	private final ExtendedGroovyClassLoader defaultScopeGroovyClassLoader = new ExtendedGroovyClassLoader(
			GroovyCompilerScope.DEFAULT);

	@Test
	void loadsGroovyFromSameClassLoader() throws Exception {
		Class<?> c1 = Class.forName("groovy.lang.Script", false, this.contextClassLoader);
		Class<?> c2 = Class.forName("groovy.lang.Script", false, this.defaultScopeGroovyClassLoader);
		assertThat(c1.getClassLoader()).isSameAs(c2.getClassLoader());
	}

	@Test
	void filtersNonGroovy() throws Exception {
		Class.forName("org.springframework.util.StringUtils", false, this.contextClassLoader);
		assertThatExceptionOfType(ClassNotFoundException.class).isThrownBy(
				() -> Class.forName("org.springframework.util.StringUtils", false, this.defaultScopeGroovyClassLoader));
	}

	@Test
	void loadsJavaTypes() throws Exception {
		Class.forName("java.lang.Boolean", false, this.defaultScopeGroovyClassLoader);
	}

	@Test
	void loadsSqlTypes() throws Exception {
		Class.forName("java.sql.SQLException", false, this.contextClassLoader);
		Class.forName("java.sql.SQLException", false, this.defaultScopeGroovyClassLoader);
	}

}
