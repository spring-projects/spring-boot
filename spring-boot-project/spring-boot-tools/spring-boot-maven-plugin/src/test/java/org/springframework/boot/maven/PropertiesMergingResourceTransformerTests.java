/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesMergingResourceTransformer}.
 *
 * @author Dave Syer
 */
class PropertiesMergingResourceTransformerTests {

	private final PropertiesMergingResourceTransformer transformer = new PropertiesMergingResourceTransformer();

	@Test
	void testProcess() throws Exception {
		assertThat(this.transformer.hasTransformedResource()).isFalse();
		this.transformer.processResource("foo", new ByteArrayInputStream("foo=bar".getBytes()), null, 0);
		assertThat(this.transformer.hasTransformedResource()).isTrue();
	}

	@Test
	void testMerge() throws Exception {
		this.transformer.processResource("foo", new ByteArrayInputStream("foo=bar".getBytes()), null, 0);
		this.transformer.processResource("bar", new ByteArrayInputStream("foo=spam".getBytes()), null, 0);
		assertThat(this.transformer.getData().getProperty("foo")).isEqualTo("bar,spam");
	}

	@Test
	void testOutput() throws Exception {
		this.transformer.setResource("foo");
		long time = 1592911068000L;
		this.transformer.processResource("foo", new ByteArrayInputStream("foo=bar".getBytes()), null, time);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JarOutputStream os = new JarOutputStream(out);
		this.transformer.modifyOutputStream(os);
		os.flush();
		os.close();
		byte[] bytes = out.toByteArray();
		assertThat(bytes).hasSizeGreaterThan(0);
		List<JarEntry> entries = new ArrayList<>();
		try (JarInputStream is = new JarInputStream(new ByteArrayInputStream(bytes))) {
			JarEntry entry;
			while ((entry = is.getNextJarEntry()) != null) {
				entries.add(entry);
			}
		}
		assertThat(entries).hasSize(1);
		assertThat(entries.get(0).getTime()).isEqualTo(time);
	}

}
