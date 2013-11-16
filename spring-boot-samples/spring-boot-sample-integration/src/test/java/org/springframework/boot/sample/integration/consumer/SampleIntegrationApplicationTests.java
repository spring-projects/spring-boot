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

package org.springframework.boot.sample.integration.consumer;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.sample.integration.SampleIntegrationApplication;
import org.springframework.boot.sample.integration.producer.ProducerApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertTrue;

/**
 * Basic integration tests for service demo application.
 * 
 * @author Dave Syer
 */
public class SampleIntegrationApplicationTests {

	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		context = SpringApplication.run(SampleIntegrationApplication.class);
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
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	private String getOutput() throws Exception {
		Future<String> future = Executors.newSingleThreadExecutor().submit(
				new Callable<String>() {
					@Override
					public String call() throws Exception {
						Resource[] resources = new Resource[0];
						while (resources.length == 0) {
							Thread.sleep(200);
							resources = ResourcePatternUtils.getResourcePatternResolver(
									new DefaultResourceLoader()).getResources(
									"file:target/output/**");
						}
						StringBuilder builder = new StringBuilder();
						for (Resource resource : resources) {
							builder.append(new String(StreamUtils
									.copyToByteArray(resource.getInputStream())));
						}
						return builder.toString();
					}
				});
		return future.get(30, TimeUnit.SECONDS);
	}
}
