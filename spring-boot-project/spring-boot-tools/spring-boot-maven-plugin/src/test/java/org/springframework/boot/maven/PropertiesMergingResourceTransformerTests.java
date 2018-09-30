/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.jar.JarOutputStream;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesMergingResourceTransformer}.
 *
 * @author Dave Syer
 */
public class PropertiesMergingResourceTransformerTests {

	private final PropertiesMergingResourceTransformer transformer = new PropertiesMergingResourceTransformer();

	@Test
	public void testProcess() throws Exception {
		assertThat(this.transformer.hasTransformedResource()).isFalse();
		this.transformer.processResource("foo",
				new ByteArrayInputStream("foo=bar".getBytes()), null);
		assertThat(this.transformer.hasTransformedResource()).isTrue();
	}

	@Test
	public void testMerge() throws Exception {
		this.transformer.processResource("foo",
				new ByteArrayInputStream("foo=bar".getBytes()), null);
		this.transformer.processResource("bar",
				new ByteArrayInputStream("foo=spam".getBytes()), null);
		assertThat(this.transformer.getData().getProperty("foo")).isEqualTo("bar,spam");
	}

	@Test
	public void testOutput() throws Exception {
		this.transformer.setResource("foo");
		this.transformer.processResource("foo",
				new ByteArrayInputStream("foo=bar".getBytes()), null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JarOutputStream os = new JarOutputStream(out);
		this.transformer.modifyOutputStream(os);
		os.flush();
		os.close();
		assertThat(out.toByteArray()).isNotNull();
		assertThat(out.toByteArray().length > 0).isTrue();
	}

}
