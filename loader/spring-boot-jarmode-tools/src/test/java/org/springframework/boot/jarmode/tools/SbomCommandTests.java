/*
 * Copyright 2012-present the original author or authors.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.jarmode.JarModeErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SbomCommand}.
 *
 * @author Hyeongjun Cho
 */
class SbomCommandTests extends AbstractJarModeTests {

	private static final String SBOM_LOCATION = "META-INF/sbom/application.cdx.json";

	private static final String SBOM_RESOURCE = "/jar-contents/application.cdx.json";

	@Test
	void shouldPrintSbom() throws IOException {
		TestPrintStream out = run(createDefaultArchive());
		assertThat(out).hasSameContentAsResource(SBOM_RESOURCE);
	}

	@Test
	void shouldPrintSbomWhenLocationIsUnderWebInfClasses() throws IOException {
		Manifest manifest = createManifest("Sbom-Location: WEB-INF/classes/META-INF/sbom/application.cdx.json");
		File archive = createArchive(manifest, "WEB-INF/classes/META-INF/sbom/application.cdx.json", SBOM_RESOURCE);
		TestPrintStream out = run(archive);
		assertThat(out).hasSameContentAsResource(SBOM_RESOURCE);
	}

	@Test
	void shouldFailWhenSbomLocationIsMissing() {
		assertThatExceptionOfType(JarModeErrorException.class).isThrownBy(() -> run(createArchive()))
			.withMessage("No SBOM found in the jar; the manifest has no 'Sbom-Location' attribute");
	}

	@Test
	void shouldFailWhenSbomEntryIsMissing() throws IOException {
		Manifest manifest = createManifest("Sbom-Location: " + SBOM_LOCATION);
		File archive = createArchive(manifest);
		assertThatExceptionOfType(JarModeErrorException.class).isThrownBy(() -> run(archive))
			.withMessage("SBOM 'META-INF/sbom/application.cdx.json' declared in the manifest was not found in the jar");
	}

	@Test
	void shouldFailWhenSbomLocationIsADirectory() throws IOException {
		Manifest manifest = createManifest("Sbom-Location: META-INF/sbom");
		File archive = createArchive(manifest, "META-INF/sbom/", "/jar-contents/empty-file");
		assertThatExceptionOfType(JarModeErrorException.class).isThrownBy(() -> run(archive))
			.withMessage("SBOM 'META-INF/sbom' declared in the manifest was not found in the jar");
	}

	@Test
	void shouldFailWhenConsoleWriteFails() throws IOException {
		SbomCommand command = new SbomCommand(new Context(createDefaultArchive(), this.tempDir));
		try (PrintStream out = new PrintStream(new FailingOutputStream())) {
			assertThatExceptionOfType(JarModeErrorException.class)
				.isThrownBy(() -> command.run(out, new ArrayDeque<>()))
				.withMessage("Failed to write the SBOM to the console");
		}
	}

	@Test
	void shouldWriteSbomToDestination() throws IOException {
		TestPrintStream out = run(createDefaultArchive(), "--destination", "application.cdx.json");
		File destination = new File(this.tempDir, "application.cdx.json");
		assertThat(destination).hasBinaryContent(getResourceContent(SBOM_RESOURCE));
		assertThat(out.toString()).isEmpty();
	}

	@Test
	void shouldWriteSbomToAbsoluteDestination() throws IOException {
		File destination = new File(this.tempDir, "absolute.cdx.json");
		run(createDefaultArchive(), "--destination", destination.getAbsolutePath());
		assertThat(destination).hasBinaryContent(getResourceContent(SBOM_RESOURCE));
	}

	@Test
	void shouldCreateMissingDestinationParentDirectories() throws IOException {
		run(createDefaultArchive(), "--destination", "reports/sbom/application.cdx.json");
		File destination = new File(this.tempDir, "reports/sbom/application.cdx.json");
		assertThat(destination).hasBinaryContent(getResourceContent(SBOM_RESOURCE));
	}

	@Test
	void shouldOverwriteExistingDestination() throws IOException {
		File destination = new File(this.tempDir, "application.cdx.json");
		Files.writeString(destination.toPath(), "stale content");
		run(createDefaultArchive(), "--destination", "application.cdx.json");
		assertThat(destination).hasBinaryContent(getResourceContent(SBOM_RESOURCE));
	}

	@Test
	void shouldFailWhenDestinationIsADirectory() throws IOException {
		File archive = createDefaultArchive();
		File destination = new File(this.tempDir, "output");
		assertThat(destination.mkdirs()).isTrue();
		assertThatExceptionOfType(JarModeErrorException.class).isThrownBy(() -> run(archive, "--destination", "output"))
			.withMessage(destination.getAbsoluteFile() + " already exists and is a directory");
	}

	private File createDefaultArchive() throws IOException {
		Manifest manifest = createManifest("Sbom-Location: " + SBOM_LOCATION);
		return createArchive(manifest, SBOM_LOCATION, SBOM_RESOURCE);
	}

	private byte[] getResourceContent(String resource) throws IOException {
		try (InputStream stream = getClass().getResourceAsStream(resource)) {
			assertThat(stream).as("Resource '%s'", resource).isNotNull();
			return stream.readAllBytes();
		}
	}

	private TestPrintStream run(File archive, String... arguments) {
		return runCommand(SbomCommand::new, archive, arguments);
	}

	static final class FailingOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			throw new IOException("Write failed");
		}

	}

}
