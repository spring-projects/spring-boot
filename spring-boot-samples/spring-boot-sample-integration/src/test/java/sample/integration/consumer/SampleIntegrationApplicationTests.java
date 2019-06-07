/*
 * Copyright 2012-2019 the original author or authors.
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

package sample.integration.consumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sample.integration.SampleIntegrationApplication;
import sample.integration.ServiceProperties;
import sample.integration.producer.ProducerApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for service demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class SampleIntegrationApplicationTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	public void stop() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testVanillaExchange(@TempDir Path temp) throws Exception {
		File inputDir = new File(temp.toFile(), "input");
		File outputDir = new File(temp.toFile(), "output");
		this.context = SpringApplication.run(SampleIntegrationApplication.class, "--service.input-dir=" + inputDir,
				"--service.output-dir=" + outputDir);
		SpringApplication.run(ProducerApplication.class, "World", "--service.input-dir=" + inputDir,
				"--service.output-dir=" + outputDir);
		String output = getOutput(outputDir);
		assertThat(output).contains("Hello World");
	}

	@Test
	void testMessageGateway(@TempDir Path temp) throws Exception {
		File inputDir = new File(temp.toFile(), "input");
		File outputDir = new File(temp.toFile(), "output");
		this.context = SpringApplication.run(SampleIntegrationApplication.class, "testviamg",
				"--service.input-dir=" + inputDir, "--service.output-dir=" + outputDir);
		String output = getOutput(this.context.getBean(ServiceProperties.class).getOutputDir());
		assertThat(output).contains("testviamg");
	}

	private String getOutput(File outputDir) throws Exception {
		Future<String> future = Executors.newSingleThreadExecutor().submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				Resource[] resources = getResourcesWithContent(outputDir);
				while (resources.length == 0) {
					Thread.sleep(200);
					resources = getResourcesWithContent(outputDir);
				}
				StringBuilder builder = new StringBuilder();
				for (Resource resource : resources) {
					try (InputStream inputStream = resource.getInputStream()) {
						builder.append(new String(StreamUtils.copyToByteArray(inputStream)));
					}
				}
				return builder.toString();
			}
		});
		return future.get(30, TimeUnit.SECONDS);
	}

	private Resource[] getResourcesWithContent(File outputDir) throws IOException {
		Resource[] candidates = ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader())
				.getResources("file:" + outputDir.getAbsolutePath() + "/**");
		for (Resource candidate : candidates) {
			if ((candidate.getFilename() != null && candidate.getFilename().endsWith(".writing"))
					|| candidate.contentLength() == 0) {
				return new Resource[0];
			}
		}
		return candidates;
	}

}
