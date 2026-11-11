/*
 * Copyright 2012-present the original author or authors.
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
