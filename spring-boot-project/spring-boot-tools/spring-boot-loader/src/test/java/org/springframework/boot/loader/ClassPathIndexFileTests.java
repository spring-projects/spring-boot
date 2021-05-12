/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassPathIndexFile}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ClassPathIndexFileTests {

	@TempDir
	File temp;

	@Test
	void loadIfPossibleWhenRootIsNotFileReturnsNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ClassPathIndexFile.loadIfPossible(new URL("https://example.com/file"), "test.idx"))
				.withMessage("URL does not reference a file");
	}

	@Test
	void loadIfPossibleWhenRootDoesNotExistReturnsNull() throws Exception {
		File root = new File(this.temp, "missing");
		assertThat(ClassPathIndexFile.loadIfPossible(root.toURI().toURL(), "test.idx")).isNull();
	}

	@Test
	void loadIfPossibleWhenRootIsDirectoryThrowsException() throws Exception {
		File root = new File(this.temp, "directory");
		root.mkdirs();
		assertThat(ClassPathIndexFile.loadIfPossible(root.toURI().toURL(), "test.idx")).isNull();
	}

	@Test
	void loadIfPossibleReturnsInstance() throws Exception {
		ClassPathIndexFile indexFile = copyAndLoadTestIndexFile();
		assertThat(indexFile).isNotNull();
	}

	@Test
	void sizeReturnsNumberOfLines() throws Exception {
		ClassPathIndexFile indexFile = copyAndLoadTestIndexFile();
		assertThat(indexFile.size()).isEqualTo(5);
	}

	@Test
	void getUrlsReturnsUrls() throws Exception {
		ClassPathIndexFile indexFile = copyAndLoadTestIndexFile();
		List<URL> urls = indexFile.getUrls();
		List<File> expected = new ArrayList<>();
		expected.add(new File(this.temp, "BOOT-INF/layers/one/lib/a.jar"));
		expected.add(new File(this.temp, "BOOT-INF/layers/one/lib/b.jar"));
		expected.add(new File(this.temp, "BOOT-INF/layers/one/lib/c.jar"));
		expected.add(new File(this.temp, "BOOT-INF/layers/two/lib/d.jar"));
		expected.add(new File(this.temp, "BOOT-INF/layers/two/lib/e.jar"));
		assertThat(urls).containsExactly(expected.stream().map(this::toUrl).toArray(URL[]::new));
	}

	private URL toUrl(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ClassPathIndexFile copyAndLoadTestIndexFile() throws IOException {
		copyTestIndexFile();
		ClassPathIndexFile indexFile = ClassPathIndexFile.loadIfPossible(this.temp.toURI().toURL(), "test.idx");
		return indexFile;
	}

	private void copyTestIndexFile() throws IOException {
		Files.copy(getClass().getResourceAsStream("classpath-index-file.idx"),
				new File(this.temp, "test.idx").toPath());
	}

}
