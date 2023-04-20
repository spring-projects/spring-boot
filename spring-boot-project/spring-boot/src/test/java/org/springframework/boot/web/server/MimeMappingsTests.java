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

package org.springframework.boot.web.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.web.server.MimeMappings.DefaultMimeMappings;
import org.springframework.boot.web.server.MimeMappings.Mapping;
import org.springframework.boot.web.server.MimeMappings.MimeMappingsRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MimeMappings}.
 *
 * @author Phillip Webb
 * @author Guirong Hu
 */
class MimeMappingsTests {

	@Test
	void defaultsCannotBeModified() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> MimeMappings.DEFAULT.add("foo", "foo/bar"));
	}

	@Test
	void createFromExisting() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		MimeMappings clone = new MimeMappings(mappings);
		mappings.add("baz", "bar");
		assertThat(clone.get("foo")).isEqualTo("bar");
		assertThat(clone.get("baz")).isNull();
	}

	@Test
	void createFromMap() {
		Map<String, String> mappings = new HashMap<>();
		mappings.put("foo", "bar");
		MimeMappings clone = new MimeMappings(mappings);
		mappings.put("baz", "bar");
		assertThat(clone.get("foo")).isEqualTo("bar");
		assertThat(clone.get("baz")).isNull();
	}

	@Test
	void iterate() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		mappings.add("baz", "boo");
		List<MimeMappings.Mapping> mappingList = new ArrayList<>();
		for (MimeMappings.Mapping mapping : mappings) {
			mappingList.add(mapping);
		}
		assertThat(mappingList.get(0).getExtension()).isEqualTo("foo");
		assertThat(mappingList.get(0).getMimeType()).isEqualTo("bar");
		assertThat(mappingList.get(1).getExtension()).isEqualTo("baz");
		assertThat(mappingList.get(1).getMimeType()).isEqualTo("boo");
	}

	@Test
	void getAll() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		mappings.add("baz", "boo");
		List<MimeMappings.Mapping> mappingList = new ArrayList<>(mappings.getAll());
		assertThat(mappingList.get(0).getExtension()).isEqualTo("foo");
		assertThat(mappingList.get(0).getMimeType()).isEqualTo("bar");
		assertThat(mappingList.get(1).getExtension()).isEqualTo("baz");
		assertThat(mappingList.get(1).getMimeType()).isEqualTo("boo");
	}

	@Test
	void addNew() {
		MimeMappings mappings = new MimeMappings();
		assertThat(mappings.add("foo", "bar")).isNull();
	}

	@Test
	void addReplacesExisting() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		assertThat(mappings.add("foo", "baz")).isEqualTo("bar");
	}

	@Test
	void remove() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		assertThat(mappings.remove("foo")).isEqualTo("bar");
		assertThat(mappings.remove("foo")).isNull();
	}

	@Test
	void get() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		assertThat(mappings.get("foo")).isEqualTo("bar");
	}

	@Test
	void getMissing() {
		MimeMappings mappings = new MimeMappings();
		assertThat(mappings.get("foo")).isNull();
	}

	@Test
	void makeUnmodifiable() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		MimeMappings unmodifiable = MimeMappings.unmodifiableMappings(mappings);
		try {
			unmodifiable.remove("foo");
		}
		catch (UnsupportedOperationException ex) {
			// Expected
		}
		mappings.remove("foo");
		assertThat(unmodifiable.get("foo")).isNull();
	}

	@Test
	void mimeTypesInDefaultMappingsAreCorrectlyStructured() {
		String regName = "[A-Za-z0-9!#$&.+\\-^_]{1,127}";
		Pattern pattern = Pattern.compile("^" + regName + "\\/" + regName + "$");
		assertThat(MimeMappings.DEFAULT).allSatisfy((mapping) -> assertThat(mapping.getMimeType()).matches(pattern));
	}

	@Test
	void getCommonTypeOnDefaultMimeMappingsDoesNotLoadMappings() {
		DefaultMimeMappings mappings = new DefaultMimeMappings();
		assertThat(mappings.get("json")).isEqualTo("application/json");
		assertThat((Object) mappings).extracting("loaded").isNull();
	}

	@Test
	void getExoticTypeOnDefaultMimeMappingsLoadsMappings() {
		DefaultMimeMappings mappings = new DefaultMimeMappings();
		assertThat(mappings.get("123")).isEqualTo("application/vnd.lotus-1-2-3");
		assertThat((Object) mappings).extracting("loaded").isNotNull();
	}

	@Test
	void iterateOnDefaultMimeMappingsLoadsMappings() {
		DefaultMimeMappings mappings = new DefaultMimeMappings();
		assertThat(mappings).isNotEmpty();
		assertThat((Object) mappings).extracting("loaded").isNotNull();
	}

	@Test
	void commonMappingsAreSubsetOfAllMappings() {
		MimeMappings defaultMappings = new DefaultMimeMappings();
		MimeMappings commonMappings = (MimeMappings) ReflectionTestUtils.getField(DefaultMimeMappings.class, "COMMON");
		for (Mapping commonMapping : commonMappings) {
			assertThat(defaultMappings.get(commonMapping.getExtension())).isEqualTo(commonMapping.getMimeType());
		}
	}

	@Test
	void lazyCopyWhenNotMutatedDelegates() {
		DefaultMimeMappings mappings = new DefaultMimeMappings();
		MimeMappings lazyCopy = MimeMappings.lazyCopy(mappings);
		assertThat(lazyCopy.get("json")).isEqualTo("application/json");
		assertThat((Object) mappings).extracting("loaded").isNull();
	}

	@Test
	void lazyCopyWhenMutatedCreatesCopy() {
		DefaultMimeMappings mappings = new DefaultMimeMappings();
		MimeMappings lazyCopy = MimeMappings.lazyCopy(mappings);
		lazyCopy.add("json", "other/json");
		assertThat(lazyCopy.get("json")).isEqualTo("other/json");
		assertThat((Object) mappings).extracting("loaded").isNotNull();
	}

	@Test
	void lazyCopyWhenMutatedCreatesCopyOnlyOnce() {
		MimeMappings mappings = new MimeMappings();
		mappings.add("json", "one/json");
		MimeMappings lazyCopy = MimeMappings.lazyCopy(mappings);
		lazyCopy.add("first", "copy/yes");
		assertThat(lazyCopy.get("json")).isEqualTo("one/json");
		mappings.add("json", "two/json");
		lazyCopy.add("second", "copy/no");
		assertThat(lazyCopy.get("json")).isEqualTo("one/json");
	}

	@Test
	void mimeMappingsMatchesTomcatDefaults() throws IOException {
		Properties ourDefaultMimeMappings = PropertiesLoaderUtils
			.loadProperties(new ClassPathResource("mime-mappings.properties", getClass()));
		Properties tomcatDefaultMimeMappings = PropertiesLoaderUtils
			.loadProperties(new ClassPathResource("MimeTypeMappings.properties", Tomcat.class));
		assertThat(ourDefaultMimeMappings).isEqualTo(tomcatDefaultMimeMappings);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new MimeMappingsRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource()
			.forResource("org/springframework/boot/web/server/mime-mappings.properties")).accepts(runtimeHints);
	}

}
