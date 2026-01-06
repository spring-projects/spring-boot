/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DocumentRoot}.
 *
 * @author Phillip Webb
 */
class DocumentRootTests {

	private static final Log logger = LogFactory.getLog(DocumentRootTests.class);

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File tempDir;

	private final DocumentRoot documentRoot = new DocumentRoot(logger);

	@Test
	void explodedWarFileDocumentRootWhenRunningFromExplodedWar() throws Exception {
		File codeSourceFile = new File(this.tempDir, "test.war/WEB-INF/lib/spring-boot.jar");
		codeSourceFile.getParentFile().mkdirs();
		codeSourceFile.createNewFile();
		File directory = this.documentRoot.getExplodedWarFileDocumentRoot(codeSourceFile);
		assertThat(directory).isEqualTo(codeSourceFile.getParentFile().getParentFile().getParentFile());
	}

	@Test
	void explodedWarFileDocumentRootWhenRunningFromPackagedWar() {
		File codeSourceFile = new File(this.tempDir, "test.war");
		File directory = this.documentRoot.getExplodedWarFileDocumentRoot(codeSourceFile);
		assertThat(directory).isNull();
	}

	@Test
	void codeSourceArchivePath() throws Exception {
		CodeSource codeSource = new CodeSource(new URL("file", "", "/some/test/path/"), (Certificate[]) null);
		File codeSourceArchive = this.documentRoot.getCodeSourceArchive(codeSource);
		assertThat(codeSourceArchive).isEqualTo(new File("/some/test/path/"));
	}

	@Test
	void codeSourceArchivePathContainingSpaces() throws Exception {
		CodeSource codeSource = new CodeSource(new URL("file", "", "/test/path/with%20space/"), (Certificate[]) null);
		File codeSourceArchive = this.documentRoot.getCodeSourceArchive(codeSource);
		assertThat(codeSourceArchive).isEqualTo(new File("/test/path/with space/"));
	}

	@Test
	void getValidDirectoryWhenHasSrcMainWebApp() {
		Map<String, String> systemEnvironment = new HashMap<>();
		File directory = new File(this.tempDir, "src/main/webapp");
		directory.mkdirs();
		DocumentRoot documentRoot = new DocumentRoot(logger, this.tempDir, systemEnvironment::get);
		assertThat(documentRoot.getValidDirectory()).isEqualTo(directory);
	}

	@Test
	void getValidDirectoryWhenHasCustomSrcMainWebApp() {
		Map<String, String> systemEnvironment = new HashMap<>();
		systemEnvironment.put("WAR_SOURCE_DIRECTORY", "src/main/unusual");
		File directory = new File(this.tempDir, "src/main/unusual");
		directory.mkdirs();
		DocumentRoot documentRoot = new DocumentRoot(logger, this.tempDir, systemEnvironment::get);
		assertThat(documentRoot.getValidDirectory()).isEqualTo(directory);
	}

}
