/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.zip.ZipContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetaInfVersionsInfo}.
 *
 * @author Phillip Webb
 */
class MetaInfVersionsInfoTests {

	@Test
	void getParsesVersionsAndEntries() {
		List<ZipContent.Entry> entries = new ArrayList<>();
		entries.add(mockEntry("META-INF/"));
		entries.add(mockEntry("META-INF/MANIFEST.MF"));
		entries.add(mockEntry("META-INF/versions/"));
		entries.add(mockEntry("META-INF/versions/9/"));
		entries.add(mockEntry("META-INF/versions/9/Foo.class"));
		entries.add(mockEntry("META-INF/versions/11/"));
		entries.add(mockEntry("META-INF/versions/11/Foo.class"));
		entries.add(mockEntry("META-INF/versions/10/"));
		entries.add(mockEntry("META-INF/versions/10/Foo.class"));
		MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
		assertThat(info.versions()).containsExactly(9, 10, 11);
		assertThat(info.directories()).containsExactly("META-INF/versions/9/", "META-INF/versions/10/",
				"META-INF/versions/11/");
	}

	@Test
	void getWhenHasBadEntryParsesGoodVersionsAndEntries() {
		List<ZipContent.Entry> entries = new ArrayList<>();
		entries.add(mockEntry("META-INF/versions/9/Foo.class"));
		entries.add(mockEntry("META-INF/versions/0x11/Foo.class"));
		MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
		assertThat(info.versions()).containsExactly(9);
		assertThat(info.directories()).containsExactly("META-INF/versions/9/");
	}

	@Test
	void getWhenHasNoEntriesReturnsNone() {
		List<ZipContent.Entry> entries = new ArrayList<>();
		MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
		assertThat(info.versions()).isEmpty();
		assertThat(info.directories()).isEmpty();
		assertThat(info).isSameAs(MetaInfVersionsInfo.NONE);
	}

	@Test
	void toleratesUnexpectedFileEntryInMetaInfVersions() {
		List<ZipContent.Entry> entries = new ArrayList<>();
		entries.add(mockEntry("META-INF/"));
		entries.add(mockEntry("META-INF/MANIFEST.MF"));
		entries.add(mockEntry("META-INF/versions/"));
		entries.add(mockEntry("META-INF/versions/unexpected"));
		entries.add(mockEntry("META-INF/versions/9/"));
		entries.add(mockEntry("META-INF/versions/9/Foo.class"));
		MetaInfVersionsInfo info = MetaInfVersionsInfo.get(entries.size(), entries::get);
		assertThat(info.versions()).containsExactly(9);
		assertThat(info.directories()).containsExactly("META-INF/versions/9/");
	}

	private ZipContent.Entry mockEntry(String name) {
		ZipContent.Entry entry = mock(ZipContent.Entry.class);
		given(entry.getName()).willReturn(name);
		given(entry.hasNameStartingWith(any()))
			.willAnswer((invocation) -> name.startsWith(invocation.getArgument(0, CharSequence.class).toString()));
		given(entry.isDirectory()).willAnswer((invocation) -> name.endsWith("/"));
		return entry;
	}

}
