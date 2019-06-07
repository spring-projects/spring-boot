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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;

import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DocumentRoot}.
 *
 * @author Phillip Webb
 */
public class DocumentRootTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private DocumentRoot documentRoot = new DocumentRoot(LogFactory.getLog(getClass()));

	@Test
	public void explodedWarFileDocumentRootWhenRunningFromExplodedWar() throws Exception {
		File webInfClasses = this.temporaryFolder.newFolder("test.war", "WEB-INF", "lib", "spring-boot.jar");
		File directory = this.documentRoot.getExplodedWarFileDocumentRoot(webInfClasses);
		assertThat(directory).isEqualTo(webInfClasses.getParentFile().getParentFile().getParentFile());
	}

	@Test
	public void explodedWarFileDocumentRootWhenRunningFromPackagedWar() throws Exception {
		File codeSourceFile = this.temporaryFolder.newFile("test.war");
		File directory = this.documentRoot.getExplodedWarFileDocumentRoot(codeSourceFile);
		assertThat(directory).isNull();
	}

	@Test
	public void codeSourceArchivePath() throws Exception {
		CodeSource codeSource = new CodeSource(new URL("file", "", "/some/test/path/"), (Certificate[]) null);
		File codeSourceArchive = this.documentRoot.getCodeSourceArchive(codeSource);
		assertThat(codeSourceArchive).isEqualTo(new File("/some/test/path/"));
	}

	@Test
	public void codeSourceArchivePathContainingSpaces() throws Exception {
		CodeSource codeSource = new CodeSource(new URL("file", "", "/test/path/with%20space/"), (Certificate[]) null);
		File codeSourceArchive = this.documentRoot.getCodeSourceArchive(codeSource);
		assertThat(codeSourceArchive).isEqualTo(new File("/test/path/with space/"));
	}

}
