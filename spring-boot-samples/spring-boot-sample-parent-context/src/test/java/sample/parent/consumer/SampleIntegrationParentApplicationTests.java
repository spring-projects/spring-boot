/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.parent.consumer;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import sample.parent.SampleParentContextApplication;
import sample.parent.producer.ProducerApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.fail;

/**
 * Basic integration tests for service demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class SampleIntegrationParentApplicationTests {

	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		context = SpringApplication.run(SampleParentContextApplication.class);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testVanillaExchange() throws Exception {
		SpringApplication.run(ProducerApplication.class, "World");
		awaitOutputContaining("Hello World");
	}

	private void awaitOutputContaining(final String requiredContents) throws Exception {
		long endTime = System.currentTimeMillis() + 30000;
		String output = null;
		while (System.currentTimeMillis() < endTime) {
			Resource[] resources = findResources();
			if (resources.length == 0) {
				Thread.sleep(200);
				resources = findResources();
			}
			else {
				output = readResources(resources);
				if (output != null && output.contains(requiredContents)) {
					return;
				}
				else {
					Thread.sleep(200);
					output = readResources(resources);
				}
			}
		}
		fail("Timed out awaiting output containing '" + requiredContents
				+ "'. Output was '" + output + "'");
	}

	private Resource[] findResources() throws IOException {
		return ResourcePatternUtils
				.getResourcePatternResolver(new DefaultResourceLoader())
				.getResources("file:target/output/**/*.msg");
	}

	private String readResources(Resource[] resources) throws IOException {
		StringBuilder builder = new StringBuilder();
		for (Resource resource : resources) {
			builder.append(
					new String(StreamUtils.copyToByteArray(resource.getInputStream())));
		}
		return builder.toString();
	}

}
