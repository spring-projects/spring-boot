/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.parent.consumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import smoketest.parent.SampleParentContextApplication;
import smoketest.parent.producer.ProducerApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.StreamUtils;

import static org.hamcrest.Matchers.containsString;

/**
 * Basic integration tests for service demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class SampleIntegrationParentApplicationTests {

	@Test
	void testVanillaExchange(@TempDir Path temp) {
		File inputDir = new File(temp.toFile(), "input");
		File outputDir = new File(temp.toFile(), "output");
		try (ConfigurableApplicationContext app = SpringApplication.run(SampleParentContextApplication.class,
				"--service.input-dir=" + inputDir, "--service.output-dir=" + outputDir)) {
			try (ConfigurableApplicationContext producer = SpringApplication.run(ProducerApplication.class,
					"--service.input-dir=" + inputDir, "--service.output-dir=" + outputDir, "World")) {
				awaitOutputContaining(outputDir, "Hello World");
			}
		}
	}

	private void awaitOutputContaining(File outputDir, String requiredContents) {
		Awaitility.waitAtMost(Duration.ofSeconds(30)).until(() -> outputIn(outputDir),
				containsString(requiredContents));
	}

	private String outputIn(File outputDir) throws IOException {
		Resource[] resources = findResources(outputDir);
		if (resources.length == 0) {
			return null;
		}
		return readResources(resources);
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
