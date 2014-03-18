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

package org.springframework.boot.loader.archive;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Dave Syer
 */
public class SpecialArchiveTests {

	@Test
	public void getEntriesForRoot() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(new File("/"), false);
		Map<String, Archive.Entry> entries = getEntriesMap(archive);
		assertThat(entries.size(), greaterThan(1));
	}

	@Test
	public void getManifest() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(new File("src/test/resources/root"));
		assertNotNull(archive.getManifest());
		Map<String, Archive.Entry> entries = getEntriesMap(archive);
		assertThat(entries.size(), equalTo(4));
	}

	@Test
	public void getManifestEvenIfNonRecursive() throws Exception {
		ExplodedArchive archive = new ExplodedArchive(
				new File("src/test/resources/root"), false);
		assertNotNull(archive.getManifest());
		Map<String, Archive.Entry> entries = getEntriesMap(archive);
		assertThat(entries.size(), equalTo(3));
	}

	private Map<String, Archive.Entry> getEntriesMap(Archive archive) {
		Map<String, Archive.Entry> entries = new HashMap<String, Archive.Entry>();
		for (Archive.Entry entry : archive.getEntries()) {
			entries.put(entry.getName().toString(), entry);
		}
		return entries;
	}
}
