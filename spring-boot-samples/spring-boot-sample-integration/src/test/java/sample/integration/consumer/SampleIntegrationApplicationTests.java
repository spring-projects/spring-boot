/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.integration.consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import sample.integration.producer.ProducerApplication;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for service demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleIntegrationApplicationTests {

	@Autowired
	private ProducerApplication producerApplication;

	@Before
	public void deleteOutput() {
		FileSystemUtils.deleteRecursively(new File("target/output"));
	}

	@Test
	public void testVanillaExchange() throws Exception {
		this.producerApplication.run("World");
		String output = getOutput();
		assertThat(output).contains("Hello World");
	}

	private String getOutput() throws Exception {
		Future<String> future = Executors.newSingleThreadExecutor()
				.submit(new Callable<String>() {
					@Override
					public String call() throws Exception {
						Resource[] resources = getResourcesWithContent();
						while (resources.length == 0) {
							Thread.sleep(200);
							resources = getResourcesWithContent();
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

	private Resource[] getResourcesWithContent() throws IOException {
		Resource[] candidates = ResourcePatternUtils
				.getResourcePatternResolver(new DefaultResourceLoader())
				.getResources("file:target/output/**");
		for (Resource candidate : candidates) {
			if (candidate.contentLength() == 0) {
				return new Resource[0];
			}
		}
		return candidates;
	}
}
