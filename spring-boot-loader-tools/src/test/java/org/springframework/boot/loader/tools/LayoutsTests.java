/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.LibraryScope;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Layouts}.
 * 
 * @author Phillip Webb
 */
public class LayoutsTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void jarFile() throws Exception {
		assertThat(Layouts.forFile(new File("test.jar")), instanceOf(Layouts.Jar.class));
		assertThat(Layouts.forFile(new File("test.JAR")), instanceOf(Layouts.Jar.class));
		assertThat(Layouts.forFile(new File("test.jAr")), instanceOf(Layouts.Jar.class));
		assertThat(Layouts.forFile(new File("te.st.jar")), instanceOf(Layouts.Jar.class));
	}

	@Test
	public void warFile() throws Exception {
		assertThat(Layouts.forFile(new File("test.war")), instanceOf(Layouts.War.class));
		assertThat(Layouts.forFile(new File("test.WAR")), instanceOf(Layouts.War.class));
		assertThat(Layouts.forFile(new File("test.wAr")), instanceOf(Layouts.War.class));
		assertThat(Layouts.forFile(new File("te.st.war")), instanceOf(Layouts.War.class));
	}

	@Test
	public void unknownFile() throws Exception {
		this.thrown.equals(IllegalStateException.class);
		this.thrown.expectMessage("Unable to deduce layout for 'test.txt'");
		Layouts.forFile(new File("test.txt"));
	}

	@Test
	public void jarLayout() throws Exception {
		Layout layout = new Layouts.Jar();
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.COMPILE),
				equalTo("lib/"));
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.PROVIDED),
				equalTo("lib/"));
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.RUNTIME),
				equalTo("lib/"));
	}

	@Test
	public void warLayout() throws Exception {
		Layout layout = new Layouts.War();
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.COMPILE),
				equalTo("WEB-INF/lib/"));
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.PROVIDED),
				equalTo("WEB-INF/lib-provided/"));
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.RUNTIME),
				equalTo("WEB-INF/lib/"));
	}

}
