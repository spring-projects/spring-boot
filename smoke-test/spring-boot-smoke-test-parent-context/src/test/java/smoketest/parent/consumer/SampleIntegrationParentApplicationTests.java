/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.parent.consumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;

import org.awaitility.Awaitility;
import org.jspecify.annotations.Nullable;
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
		Awaitility.waitAtMost(Duration.ofSeconds(30))
			.until(() -> outputIn(outputDir), containsString(requiredContents));
	}

	private @Nullable String outputIn(File outputDir) throws IOException {
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
