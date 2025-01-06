/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.function.Consumer;
import java.util.jar.JarFile;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.testsupport.TestJar;
import org.springframework.boot.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UrlJarFileFactory}.
 *
 * @author Phillip Webb
 */
@AssertFileChannelDataBlocksClosed
class UrlJarFileFactoryTests {

	@TempDir
	File temp;

	private final UrlJarFileFactory factory = new UrlJarFileFactory();

	@Mock
	private Consumer<JarFile> closeAction;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void createJarFileWhenLocalFile() throws Throwable {
		File file = new File(this.temp, "test.jar");
		TestJar.create(file);
		URL url = file.toURI().toURL();
		JarFile jarFile = this.factory.createJarFile(url, this.closeAction);
		assertThat(jarFile).isInstanceOf(UrlJarFile.class);
		assertThat(jarFile).hasFieldOrPropertyWithValue("closeAction", this.closeAction);
	}

	@Test
	void createJarFileWhenNested() throws Throwable {
		File file = new File(this.temp, "test.jar");
		TestJar.create(file);
		URL url = new URL("nested:" + file.getPath() + "/!nested.jar");
		JarFile jarFile = this.factory.createJarFile(url, this.closeAction);
		assertThat(jarFile).isInstanceOf(UrlNestedJarFile.class);
		assertThat(jarFile).hasFieldOrPropertyWithValue("closeAction", this.closeAction);
	}

	@Test
	void createJarFileWhenStream() throws Exception {
		File file = new File(this.temp, "test.jar");
		TestJar.create(file);
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/test", (exchange) -> {
			exchange.sendResponseHeaders(200, file.length());
			try (InputStream in = new FileInputStream(file)) {
				in.transferTo(exchange.getResponseBody());
			}
			exchange.close();
		});
		server.start();
		try {
			URL url = new URL("http://localhost:" + server.getAddress().getPort() + "/test");
			JarFile jarFile = this.factory.createJarFile(url, this.closeAction);
			assertThat(jarFile).isInstanceOf(UrlJarFile.class);
			assertThat(jarFile).hasFieldOrPropertyWithValue("closeAction", this.closeAction);
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void createWhenHasRuntimeRef() {

	}

}
