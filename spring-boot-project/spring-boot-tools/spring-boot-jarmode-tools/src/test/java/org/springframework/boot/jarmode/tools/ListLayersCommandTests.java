/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.jarmode.JarModeErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ListLayersCommand}.
 *
 * @author Moritz Halbritter
 */
class ListLayersCommandTests extends AbstractJarModeTests {

	@Test
	void shouldListLayers() throws IOException {
		Manifest manifest = createManifest("Spring-Boot-Layers-Index: META-INF/layers.idx");
		TestPrintStream out = run(createArchive(manifest, "META-INF/layers.idx", "/jar-contents/layers.idx"));
		assertThat(out).hasSameContentAsResource("list-layers-output.txt");
	}

	@Test
	void shouldFailWhenLayersAreNotEnabled() {
		assertThatExceptionOfType(JarModeErrorException.class).isThrownBy(() -> run(createArchive()))
			.withMessage("Layers are not enabled");
	}

	private TestPrintStream run(File archive) {
		return runCommand(ListLayersCommand::new, archive);
	}

}
