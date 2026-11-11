/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;

/**
 * Integration tests for {@link DockerApi}.
 *
 * @author Phillip Webb
 */
@DisabledIfDockerUnavailable
class DockerApiIntegrationTests {

	private final DockerApi docker = new DockerApi();

	@Test
	void pullImage() throws IOException {
		this.docker.image()
			.pull(ImageReference.of("docker.io/paketobuildpacks/builder:base"), null,
					new TotalProgressPullListener(new TotalProgressBar("Pulling: ")));
	}

}
