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

package sample.parent.consumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sample.parent.SampleParentContextApplication;
import sample.parent.producer.ProducerApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Basic integration tests for service demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class SampleIntegrationParentApplicationTests {

	@Test
	void testVanillaExchange(@TempDir Path temp) throws Exception {
		File inputDir = new File(temp.toFile(), "input");
		File outputDir = new File(temp.toFile(), "output");
		ConfigurableApplicationContext app = SpringApplication.run(SampleParentContextApplication.class,
				"--service.input-dir=" + inputDir, "--service.output-dir=" + outputDir);
		try {
			ConfigurableApplicationContext producer = SpringApplication.run(ProducerApplication.class,
					"--service.input-dir=" + inputDir, "--service.output-dir=" + outputDir, "World");
			try {
				awaitOutputContaining(outputDir, "Hello World");
			}
			finally {
				producer.close();
			}
		}
		finally {
			app.close();
		}
	}

	private void awaitOutputContaining(File outputDir, String requiredContents) throws Exception {
		long endTime = System.currentTimeMillis() + 30000;
		String output = null;
		while (System.currentTimeMillis() < endTime) {
			Resource[] resources = findResources(outputDir);
			if (resources.length == 0) {
				Thread.sleep(200);
				resources = findResources(outputDir);
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
		fail("Timed out awaiting output containing '" + requiredContents + "'. Output was '" + output + "'");
	}

	private Resource[] findResources(File outputDir) throws IOException {
		return ResourcePatternUtils.getResourcePatternResolver(new DefaultResourceLoader())
				.getResources("file:" + outputDir.getAbsolutePath() + "/*.txt");
	}

	private String readResources(Resource[] resources) throws IOException {
		StringBuilder builder = new StringBuilder();
		for (Resource resource : resources) {
			try (InputStream input = resource.getInputStream()) {
				builder.append(new String(StreamUtils.copyToByteArray(input)));
			}
		}
		return builder.toString();
	}

}
