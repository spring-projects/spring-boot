/*
 * Copyright 2012-present the original author or authors.
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
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.COMPILE)).isEqualTo("BOOT-INF/lib/");
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.CUSTOM)).isEqualTo("BOOT-INF/lib/");
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.PROVIDED)).isEqualTo("BOOT-INF/lib/");
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.RUNTIME)).isEqualTo("BOOT-INF/lib/");
	}

	@Test
	void warLayout() {
		Layout layout = new Layouts.War();
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.COMPILE)).isEqualTo("WEB-INF/lib/");
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.CUSTOM)).isEqualTo("WEB-INF/lib/");
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.PROVIDED)).isEqualTo("WEB-INF/lib-provided/");
		assertThat(layout.getLibraryLocation("lib.jar", LibraryScope.RUNTIME)).isEqualTo("WEB-INF/lib/");
	}

}
