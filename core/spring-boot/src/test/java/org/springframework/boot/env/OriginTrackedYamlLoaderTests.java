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

package org.springframework.boot.env;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.composer.ComposerException;

import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link OriginTrackedYamlLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OriginTrackedYamlLoaderTests {

	private OriginTrackedYamlLoader loader;

	private @Nullable List<Map<String, Object>> result;

	@BeforeEach
	void setUp() {
		Resource resource = new ClassPathResource("test-yaml.yml");
		this.loader = new OriginTrackedYamlLoader(resource);
	}

	@Test
	@WithTestYamlResource
	void processSimpleKey() {
		OriginTrackedValue value = getValue("name");
		assertThat(value).isNotNull();
		assertThat(value).hasToString("Martin D'vloper");
		assertThat(getLocation(value)).isEqualTo("3:7");
	}

	@Test
	@WithTestYamlResource
	void processMap() {
		OriginTrackedValue perl = getValue("languages.perl");
		OriginTrackedValue python = getValue("languages.python");
		OriginTrackedValue pascal = getValue("languages.pascal");
		assertThat(perl).isNotNull();
		assertThat(perl).hasToString("Elite");
		assertThat(getLocation(perl)).isEqualTo("13:11");
		assertThat(python).isNotNull();
		assertThat(python).hasToString("Elite");
		assertThat(getLocation(python)).isEqualTo("14:13");
		assertThat(pascal).isNotNull();
		assertThat(pascal).hasToString("Lame");
		assertThat(getLocation(pascal)).isEqualTo("15:13");
	}

	@Test
	@WithTestYamlResource
	void processCollection() {
		OriginTrackedValue apple = getValue("foods[0]");
		OriginTrackedValue orange = getValue("foods[1]");
		OriginTrackedValue strawberry = getValue("foods[2]");
		OriginTrackedValue mango = getValue("foods[3]");
		assertThat(apple).isNotNull();
		assertThat(apple).hasToString("Apple");
		assertThat(getLocation(apple)).isEqualTo("8:7");
		assertThat(orange).isNotNull();
		assertThat(orange).hasToString("Orange");
		assertThat(getLocation(orange)).isEqualTo("9:7");
		assertThat(strawberry).isNotNull();
		assertThat(strawberry).hasToString("Strawberry");
		assertThat(getLocation(strawberry)).isEqualTo("10:7");
		assertThat(mango).isNotNull();
		assertThat(mango).hasToString("Mango");
		assertThat(getLocation(mango)).isEqualTo("11:7");
	}

	@Test
	@WithTestYamlResource
	void processMultiline() {
		OriginTrackedValue education = getValue("education");
		assertThat(education).isNotNull();
		assertThat(education).hasToString("4 GCSEs\n3 A-Levels\nBSc in the Internet of Things\n");
		assertThat(getLocation(education)).isEqualTo("16:12");
	}

	@Test
	@WithTestYamlResource
	void processListOfMaps() {
		OriginTrackedValue name = getValue("example.foo[0].name");
		OriginTrackedValue url = getValue("example.foo[0].url");
		OriginTrackedValue bar1 = getValue("example.foo[0].bar[0].bar1");
		OriginTrackedValue bar2 = getValue("example.foo[0].bar[1].bar2");
		assertThat(name).isNotNull();
		assertThat(name).hasToString("springboot");
		assertThat(getLocation(name)).isEqualTo("22:15");
		assertThat(url).isNotNull();
		assertThat(url).hasToString("https://springboot.example.com/");
		assertThat(getLocation(url)).isEqualTo("23:14");
		assertThat(bar1).isNotNull();
		assertThat(bar1).hasToString("baz");
		assertThat(getLocation(bar1)).isEqualTo("25:19");
		assertThat(bar2).isNotNull();
		assertThat(bar2).hasToString("bling");
		assertThat(getLocation(bar2)).isEqualTo("26:19");
	}

	@Test
	@WithTestYamlResource
	void processEmptyAndNullValues() {
		OriginTrackedValue empty = getValue("empty");
		OriginTrackedValue nullValue = getValue("null-value");
		OriginTrackedValue emptyList = getValue("emptylist");
		assertThat(empty).isNotNull();
		assertThat(empty.getValue()).isEqualTo("");
		assertThat(getLocation(empty)).isEqualTo("27:8");
		assertThat(nullValue).isNotNull();
		assertThat(nullValue.getValue()).isEqualTo("");
		assertThat(getLocation(nullValue)).isEqualTo("28:13");
		assertThat(emptyList).isNotNull();
		assertThat(emptyList.getValue()).isEqualTo("");
		assertThat(getLocation(emptyList)).isEqualTo("29:12");
	}

	@Test
	@WithTestYamlResource
	void emptyMapsAreDropped() {
		Object emptyMap = getValue("emptymap");
		assertThat(emptyMap).isNull();
	}

	@Test
	void unsupportedType() {
		String yaml = "value: !!java.net.URL [!!java.lang.String [!!java.lang.StringBuilder [\"http://localhost:9000/\"]]]";
		Resource resource = new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8));
		this.loader = new OriginTrackedYamlLoader(resource);
		assertThatExceptionOfType(ComposerException.class).isThrownBy(this.loader::load);
	}

	@Test
	@WithResource(name = "test-empty-yaml.yml", content = """
			---
			---

			---
			---
			""")
	void emptyDocuments() {
		this.loader = new OriginTrackedYamlLoader(new ClassPathResource("test-empty-yaml.yml"));
		List<Map<String, Object>> loaded = this.loader.load();
		assertThat(loaded).isEmpty();
	}

	@Test
	void loadWhenLargeNumberOfNodesLoadsYaml() {
		StringBuilder yaml = new StringBuilder();
		int size = 500;
		yaml.append("defs:\n");
		for (int i = 0; i < size; i++) {
			yaml.append(" - def" + i + ": &def" + i + "\n");
			yaml.append("    - value: " + i + "\n");
		}
		yaml.append("refs:\n");
		for (int i = 0; i < size; i++) {
			yaml.append("  ref" + i + ":\n");
			yaml.append("   - value: *def" + i + "\n");
		}
		Resource resource = new ByteArrayResource(yaml.toString().getBytes(StandardCharsets.UTF_8));
		this.loader = new OriginTrackedYamlLoader(resource);
		Map<String, Object> loaded = this.loader.load().get(0);
		assertThat(loaded).hasSize(size * 2);
	}

	@Test
	@WithResource(name = "recursive.yml", content = """
			&def1
			*def1: a
			test:
			  a:
			    spring: 'a'
			  b:
			    boot: 'b'
			""")
	void loadWhenRecursiveLoadsYaml() {
		Resource resource = new ClassPathResource("recursive.yml");
		this.loader = new OriginTrackedYamlLoader(resource);
		Map<String, Object> loaded = this.loader.load().get(0);
		assertThat(loaded.get("test.a.spring")).hasToString("a");
		assertThat(loaded.get("test.b.boot")).hasToString("b");
	}

	@Test
	@WithResource(name = "anchors.yml", content = """
			some:
			  path: &anchor
			    config:
			      key: value
			  anotherpath:
			    <<: *anchor
			""")
	void loadWhenUsingAnchors() {
		Resource resource = new ClassPathResource("anchors.yml");
		this.loader = new OriginTrackedYamlLoader(resource);
		Map<String, Object> loaded = this.loader.load().get(0);
		assertThat(loaded.get("some.path.config.key")).hasToString("value");
		assertThat(loaded.get("some.anotherpath.config.key")).hasToString("value");
	}

	@Test
	void canLoadFilesBiggerThan3Mb() {
		StringBuilder yaml = new StringBuilder();
		while (yaml.length() < 4_194_304) {
			yaml.append("- some list entry\n");
		}
		Resource resource = new ByteArrayResource(yaml.toString().getBytes(StandardCharsets.UTF_8));
		this.loader = new OriginTrackedYamlLoader(resource);
		Map<String, Object> loaded = this.loader.load().get(0);
		assertThat(loaded).isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	private <T> @Nullable T getValue(String name) {
		if (this.result == null) {
			this.result = this.loader.load();
		}
		return (T) this.result.get(0).get(name);
	}

	private String getLocation(OriginTrackedValue value) {
		TextResourceOrigin origin = (TextResourceOrigin) value.getOrigin();
		assertThat(origin).isNotNull();
		Location location = origin.getLocation();
		assertThat(location).isNotNull();
		return location.toString();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@WithResource(name = "test-yaml.yml", content = """
			# https://docs.ansible.com/ansible/latest/reference_appendices/YAMLSyntax.html

			name: Martin D'vloper
			job: Developer
			skill: Elite
			employed: True
			foods:
			    - Apple
			    - Orange
			    - Strawberry
			    - Mango
			languages:
			    perl: Elite
			    python: Elite
			    pascal: Lame
			education: |
			    4 GCSEs
			    3 A-Levels
			    BSc in the Internet of Things
			example:
			    foo:
			      - name: springboot
			        url: https://springboot.example.com/
			        bar:
			          - bar1: baz
			          - bar2: bling
			empty: ""
			null-value: null
			emptylist: []
			emptymap: {}
			---

			spring:
			  profiles: development
			name: Test Name

			---
			""")
	private @interface WithTestYamlResource {

	}

}
