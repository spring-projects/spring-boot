/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Layouts}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class LayoutsTests {

	@Test
	void jarFile() {
		assertThat(Layouts.forFile(new File("test.jar"))).isInstanceOf(Layouts.Jar.class);
		assertThat(Layouts.forFile(new File("test.JAR"))).isInstanceOf(Layouts.Jar.class);
		assertThat(Layouts.forFile(new File("test.jAr"))).isInstanceOf(Layouts.Jar.class);
		assertThat(Layouts.forFile(new File("te.st.jar"))).isInstanceOf(Layouts.Jar.class);
	}

	@Test
	void warFile() {
		assertThat(Layouts.forFile(new File("test.war"))).isInstanceOf(Layouts.War.class);
		assertThat(Layouts.forFile(new File("test.WAR"))).isInstanceOf(Layouts.War.class);
		assertThat(Layouts.forFile(new File("test.wAr"))).isInstanceOf(Layouts.War.class);
		assertThat(Layouts.forFile(new File("te.st.war"))).isInstanceOf(Layouts.War.class);
	}

	@Test
	void unknownFile() {
		assertThatIllegalStateException().isThrownBy(() -> Layouts.forFile(new File("test.txt")))
				.withMessageContaining("Unable to deduce layout for 'test.txt'");
	}

	@Test
	void jarLayout() {
		Layout layout = new Layouts.Jar();
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.COMPILE)).isEqualTo("BOOT-INF/lib/");
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.CUSTOM)).isEqualTo("BOOT-INF/lib/");
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.PROVIDED)).isEqualTo("BOOT-INF/lib/");
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.RUNTIME)).isEqualTo("BOOT-INF/lib/");
	}

	@Test
	void warLayout() {
		Layout layout = new Layouts.War();
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.COMPILE)).isEqualTo("WEB-INF/lib/");
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.CUSTOM)).isEqualTo("WEB-INF/lib/");
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.PROVIDED)).isEqualTo("WEB-INF/lib-provided/");
		assertThat(layout.getLibraryDestination("lib.jar", LibraryScope.RUNTIME)).isEqualTo("WEB-INF/lib/");
	}

}
