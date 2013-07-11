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
package org.springframework.bootstrap.config;

import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.springframework.bootstrap.config.YamlProcessor.MatchCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 * 
 */
public class YamlProcessorTests {

	private YamlProcessor processor = new YamlProcessor();

	@Test
	public void arrayConvertedToIndexedBeanReference() {
		this.processor.setResources(new Resource[] { new ByteArrayResource(
				"foo: bar\nbar: [1,2,3]".getBytes()) });
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				assertEquals(1, properties.get("bar[0]"));
				assertEquals(2, properties.get("bar[1]"));
				assertEquals(3, properties.get("bar[2]"));
				assertEquals(4, properties.size());
			}
		});
	}

	@Test
	public void mapConvertedToIndexedBeanReference() {
		this.processor.setResources(new Resource[] { new ByteArrayResource(
				"foo: bar\nbar:\n spam: bucket".getBytes()) });
		this.processor.process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				// System.err.println(properties);
				assertEquals("bucket", properties.get("bar.spam"));
				assertEquals(2, properties.size());
			}
		});
	}

}
