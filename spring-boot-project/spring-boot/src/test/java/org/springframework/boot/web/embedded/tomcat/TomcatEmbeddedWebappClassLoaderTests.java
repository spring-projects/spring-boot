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

package org.springframework.boot.web.embedded.tomcat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.ParallelWebappClassLoader;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.catalina.webresources.WarResourceSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatEmbeddedWebappClassLoader}.
 *
 * @author Andy Wilkinson
 */
class TomcatEmbeddedWebappClassLoaderTests {

	@TempDir
	File tempDir;

	@Test
	void getResourceFindsResourceFromParentClassLoader() throws Exception {
		File war = createWar();
		withWebappClassLoader(war, (classLoader) -> assertThat(classLoader.getResource("test.txt"))
				.isEqualTo(new URL(webInfClassesUrlString(war) + "test.txt")));
	}

	@Test
	void getResourcesOnlyFindsResourcesFromParentClassLoader() throws Exception {
		File warFile = createWar();
		withWebappClassLoader(warFile, (classLoader) -> {
			List<URL> urls = new ArrayList<>();
			CollectionUtils.toIterator(classLoader.getResources("test.txt")).forEachRemaining(urls::add);
			assertThat(urls).containsExactly(new URL(webInfClassesUrlString(warFile) + "test.txt"));
		});
	}

	private void withWebappClassLoader(File war, ClassLoaderConsumer consumer) throws Exception {
		URLClassLoader parent = new URLClassLoader(new URL[] { new URL(webInfClassesUrlString(war)) }, null);
		try (ParallelWebappClassLoader classLoader = new TomcatEmbeddedWebappClassLoader(parent)) {
			StandardContext context = new StandardContext();
			context.setName("test");
			StandardRoot resources = new StandardRoot();
			resources.setContext(context);
			resources.addJarResources(new WarResourceSet(resources, "/", war.getAbsolutePath()));
			resources.start();
			classLoader.setResources(resources);
			classLoader.start();
			try {
				consumer.accept(classLoader);
			}
			finally {
				classLoader.stop();
				classLoader.close();
				resources.stop();
			}
		}
		parent.close();
	}

	private String webInfClassesUrlString(File war) {
		return "jar:file:" + war.getAbsolutePath() + "!/WEB-INF/classes/";
	}

	private File createWar() throws IOException {
		File warFile = new File(this.tempDir, "test.war");
		try (JarOutputStream warOut = new JarOutputStream(new FileOutputStream(warFile))) {
			createEntries(warOut, "WEB-INF/", "WEB-INF/classes/", "WEB-INF/classes/test.txt");
		}
		return warFile;
	}

	private void createEntries(JarOutputStream out, String... names) throws IOException {
		for (String name : names) {
			out.putNextEntry(new ZipEntry(name));
			out.closeEntry();
		}
	}

	interface ClassLoaderConsumer {

		void accept(ClassLoader classLoader) throws Exception;

	}

}
