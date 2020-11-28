/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Image}.
 *
 * @author Phillip Webb
 */
class ImageTests extends AbstractJsonTests {

	@Test
	void getConfigEnvContainsParsedValues() throws Exception {
		Image image = getImage();
		Map<String, String> env = image.getConfig().getEnv();
		assertThat(env).contains(entry("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
				entry("CNB_USER_ID", "2000"), entry("CNB_GROUP_ID", "2000"),
				entry("CNB_STACK_ID", "org.cloudfoundry.stacks.cflinuxfs3"));
	}

	@Test
	void getConfigLabelsReturnsLabels() throws Exception {
		Image image = getImage();
		Map<String, String> labels = image.getConfig().getLabels();
		assertThat(labels).contains(entry("io.buildpacks.stack.id", "org.cloudfoundry.stacks.cflinuxfs3"));
	}

	@Test
	void getLayersReturnsImageLayers() throws Exception {
		Image image = getImage();
		List<LayerId> layers = image.getLayers();
		assertThat(layers).hasSize(46);
		assertThat(layers.get(0).toString())
				.isEqualTo("sha256:733a8e5ce32984099ef675fce04730f6e2a6dcfdf5bd292fea01a8f936265342");
		assertThat(layers.get(45).toString())
				.isEqualTo("sha256:5f70bf18a086007016e948b04aed3b82103a36bea41755b6cddfaf10ace3c6ef");
	}

	@Test
	void getOsReturnsOs() throws Exception {
		Image image = getImage();
		assertThat(image.getOs()).isEqualTo("linux");
	}

	private Image getImage() throws IOException {
		return Image.of(getContent("image.json"));
	}

}
