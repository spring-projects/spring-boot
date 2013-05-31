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

package org.springframework.bootstrap.maven;

import java.io.ByteArrayOutputStream;
import java.util.jar.JarOutputStream;

import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class PropertiesMergingResourceTransformerTests {

	private PropertiesMergingResourceTransformer transformer = new PropertiesMergingResourceTransformer();

	@Test
	public void testProcess() throws Exception {
		assertFalse(this.transformer.hasTransformedResource());
		this.transformer.processResource("foo",
				new ByteArrayResource("foo=bar".getBytes()).getInputStream(), null);
		assertTrue(this.transformer.hasTransformedResource());
	}

	@Test
	public void testMerge() throws Exception {
		this.transformer.processResource("foo",
				new ByteArrayResource("foo=bar".getBytes()).getInputStream(), null);
		this.transformer.processResource("bar",
				new ByteArrayResource("foo=spam".getBytes()).getInputStream(), null);
		assertEquals("bar,spam", this.transformer.getData().getProperty("foo"));
	}

	@Test
	public void testOutput() throws Exception {
		this.transformer.resource = "foo";
		this.transformer.processResource("foo",
				new ByteArrayResource("foo=bar".getBytes()).getInputStream(), null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JarOutputStream os = new JarOutputStream(out);
		this.transformer.modifyOutputStream(os);
		os.flush();
		os.close();
		assertNotNull(out.toByteArray());
		assertTrue(out.toByteArray().length > 0);
	}

}
