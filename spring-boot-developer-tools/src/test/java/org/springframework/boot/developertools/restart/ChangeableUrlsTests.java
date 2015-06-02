/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.restart;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ChangeableUrls}.
 *
 * @author Phillip Webb
 */
public class ChangeableUrlsTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void folderUrl() throws Exception {
		URL url = makeUrl("myproject");
		assertThat(ChangeableUrls.fromUrls(url).size(), equalTo(1));
	}

	@Test
	public void fileUrl() throws Exception {
		URL url = this.temporaryFolder.newFile().toURI().toURL();
		assertThat(ChangeableUrls.fromUrls(url).size(), equalTo(0));
	}

	@Test
	public void httpUrl() throws Exception {
		URL url = new URL("http://spring.io");
		assertThat(ChangeableUrls.fromUrls(url).size(), equalTo(0));
	}

	@Test
	public void skipsUrls() throws Exception {
		ChangeableUrls urls = ChangeableUrls
				.fromUrls(makeUrl("spring-boot"), makeUrl("spring-boot-autoconfigure"),
						makeUrl("spring-boot-actuator"), makeUrl("spring-boot-starter"),
						makeUrl("spring-boot-starter-some-thing"));
		assertThat(urls.size(), equalTo(0));
	}

	private URL makeUrl(String name) throws IOException {
		File file = this.temporaryFolder.newFolder();
		file = new File(file, name);
		file = new File(file, "target");
		file = new File(file, "classes");
		file.mkdirs();
		return file.toURI().toURL();
	}

}
