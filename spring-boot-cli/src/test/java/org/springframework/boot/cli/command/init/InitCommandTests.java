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

package org.springframework.boot.cli.command.init;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import joptsimple.OptionSet;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.cli.command.status.ExitStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link InitCommand}
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class InitCommandTests extends AbstractHttpClientMockTests {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final TestableInitCommandOptionHandler handler;

	private final InitCommand command;

	@Captor
	private ArgumentCaptor<HttpUriRequest> requestCaptor;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
	}

	public InitCommandTests() {
		InitializrService initializrService = new InitializrService(this.http);
		this.handler = new TestableInitCommandOptionHandler(initializrService);
		this.command = new InitCommand(this.handler);
	}

	@Test
	public void listServiceCapabilitiesText() throws Exception {
		mockSuccessfulMetadataTextGet();
		this.command.run("--list", "--target=http://fake-service");
	}

	@Test
	public void listServiceCapabilities() throws Exception {
		mockSuccessfulMetadataGet(true);
		this.command.run("--list", "--target=http://fake-service");
	}

	@Test
	public void listServiceCapabilitiesV2() throws Exception {
		mockSuccessfulMetadataGetV2(true);
		this.command.run("--list", "--target=http://fake-service");
	}

	@Test
	public void generateProject() throws Exception {
		String fileName = UUID.randomUUID().toString() + ".zip";
		File file = new File(fileName);
		assertThat(file.exists()).as("file should not exist").isFalse();
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", fileName);
		mockSuccessfulProjectGeneration(request);
		try {
			assertThat(this.command.run()).isEqualTo(ExitStatus.OK);
			assertThat(file.exists()).as("file should have been created").isTrue();
		}
		finally {
			assertThat(file.delete()).as("failed to delete test file").isTrue();
		}
	}

	@Test
	public void generateProjectNoFileNameAvailable() throws Exception {
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", null);
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run()).isEqualTo(ExitStatus.ERROR);
	}

	@Test
	public void generateProjectAndExtract() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run("--extract", folder.getAbsolutePath()))
				.isEqualTo(ExitStatus.OK);
		File archiveFile = new File(folder, "test.txt");
		assertThat(archiveFile).exists();
	}

	@Test
	public void generateProjectAndExtractWithConvention() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run(folder.getAbsolutePath() + "/"))
				.isEqualTo(ExitStatus.OK);
		File archiveFile = new File(folder, "test.txt");
		assertThat(archiveFile).exists();
	}

	@Test
	public void generateProjectArchiveExtractedByDefault() throws Exception {
		String fileName = UUID.randomUUID().toString();
		assertThat(fileName.contains(".")).as("No dot in filename").isFalse();
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		File file = new File(fileName);
		File archiveFile = new File(file, "test.txt");
		try {
			assertThat(this.command.run(fileName)).isEqualTo(ExitStatus.OK);
			assertThat(archiveFile).exists();
		}
		finally {
			archiveFile.delete();
			file.delete();
		}
	}

	@Test
	public void generateProjectFileSavedAsFileByDefault() throws Exception {
		String fileName = UUID.randomUUID().toString();
		String content = "Fake Content";
		byte[] archive = content.getBytes();
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/octet-stream", "pom.xml", archive);
		mockSuccessfulProjectGeneration(request);
		File file = new File(fileName);
		try {
			assertThat(this.command.run(fileName)).isEqualTo(ExitStatus.OK);
			assertThat(file.exists()).as("File not saved properly").isTrue();
			assertThat(file.isFile()).as("Should not be a directory").isTrue();
		}
		finally {
			file.delete();
		}
	}

	@Test
	public void generateProjectAndExtractUnsupportedArchive() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		String fileName = UUID.randomUUID().toString() + ".zip";
		File file = new File(fileName);
		assertThat(file.exists()).as("file should not exist").isFalse();
		try {
			byte[] archive = createFakeZipArchive("test.txt", "Fake content");
			MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
					"application/foobar", fileName, archive);
			mockSuccessfulProjectGeneration(request);
			assertThat(this.command.run("--extract", folder.getAbsolutePath()))
					.isEqualTo(ExitStatus.OK);
			assertThat(file.exists()).as("file should have been saved instead").isTrue();
		}
		finally {
			assertThat(file.delete()).as("failed to delete test file").isTrue();
		}
	}

	@Test
	public void generateProjectAndExtractUnknownContentType() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		String fileName = UUID.randomUUID().toString() + ".zip";
		File file = new File(fileName);
		assertThat(file.exists()).as("file should not exist").isFalse();
		try {
			byte[] archive = createFakeZipArchive("test.txt", "Fake content");
			MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
					null, fileName, archive);
			mockSuccessfulProjectGeneration(request);
			assertThat(this.command.run("--extract", folder.getAbsolutePath()))
					.isEqualTo(ExitStatus.OK);
			assertThat(file.exists()).as("file should have been saved instead").isTrue();
		}
		finally {
			assertThat(file.delete()).as("failed to delete test file").isTrue();
		}
	}

	@Test
	public void fileNotOverwrittenByDefault() throws Exception {
		File file = this.temporaryFolder.newFile();
		long fileLength = file.length();
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", file.getAbsolutePath());
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run()).as("Should have failed")
				.isEqualTo(ExitStatus.ERROR);
		assertThat(file.length()).as("File should not have changed")
				.isEqualTo(fileLength);
	}

	@Test
	public void overwriteFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		long fileLength = file.length();
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", file.getAbsolutePath());
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run("--force")).isEqualTo(ExitStatus.OK);
		assertThat(fileLength != file.length()).as("File should have changed").isTrue();
	}

	@Test
	public void fileInArchiveNotOverwrittenByDefault() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		File conflict = new File(folder, "test.txt");
		assertThat(conflict.createNewFile()).as("Should have been able to create file")
				.isTrue();
		long fileLength = conflict.length();
		// also contains test.txt
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run("--extract", folder.getAbsolutePath()))
				.isEqualTo(ExitStatus.ERROR);
		assertThat(conflict.length()).as("File should not have changed")
				.isEqualTo(fileLength);
	}

	@Test
	public void parseProjectOptions() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("-g=org.demo", "-a=acme", "-v=1.2.3-SNAPSHOT", "-n=acme-sample",
				"--description=Acme sample project", "--package-name=demo.foo",
				"-t=ant-project", "--build=grunt", "--format=web", "-p=war", "-j=1.9",
				"-l=groovy", "-b=1.2.0.RELEASE", "-d=web,data-jpa");
		assertThat(this.handler.lastRequest.getGroupId()).isEqualTo("org.demo");
		assertThat(this.handler.lastRequest.getArtifactId()).isEqualTo("acme");
		assertThat(this.handler.lastRequest.getVersion()).isEqualTo("1.2.3-SNAPSHOT");
		assertThat(this.handler.lastRequest.getName()).isEqualTo("acme-sample");
		assertThat(this.handler.lastRequest.getDescription())
				.isEqualTo("Acme sample project");
		assertThat(this.handler.lastRequest.getPackageName()).isEqualTo("demo.foo");
		assertThat(this.handler.lastRequest.getType()).isEqualTo("ant-project");
		assertThat(this.handler.lastRequest.getBuild()).isEqualTo("grunt");
		assertThat(this.handler.lastRequest.getFormat()).isEqualTo("web");
		assertThat(this.handler.lastRequest.getPackaging()).isEqualTo("war");
		assertThat(this.handler.lastRequest.getJavaVersion()).isEqualTo("1.9");
		assertThat(this.handler.lastRequest.getLanguage()).isEqualTo("groovy");
		assertThat(this.handler.lastRequest.getBootVersion()).isEqualTo("1.2.0.RELEASE");
		List<String> dependencies = this.handler.lastRequest.getDependencies();
		assertThat(dependencies).hasSize(2);
		assertThat(dependencies.contains("web")).isTrue();
		assertThat(dependencies.contains("data-jpa")).isTrue();
	}

	@Test
	public void overwriteFileInArchive() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		File conflict = new File(folder, "test.txt");
		assertThat(conflict.createNewFile()).as("Should have been able to create file")
				.isTrue();
		long fileLength = conflict.length();
		// also contains test.txt
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertThat(this.command.run("--force", "--extract", folder.getAbsolutePath()))
				.isEqualTo(ExitStatus.OK);
		assertThat(fileLength != conflict.length()).as("File should have changed")
				.isTrue();
	}

	@Test
	public void parseTypeOnly() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("-t=ant-project");
		assertThat(this.handler.lastRequest.getBuild()).isEqualTo("maven");
		assertThat(this.handler.lastRequest.getFormat()).isEqualTo("project");
		assertThat(this.handler.lastRequest.isDetectType()).isFalse();
		assertThat(this.handler.lastRequest.getType()).isEqualTo("ant-project");
	}

	@Test
	public void parseBuildOnly() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("--build=ant");
		assertThat(this.handler.lastRequest.getBuild()).isEqualTo("ant");
		assertThat(this.handler.lastRequest.getFormat()).isEqualTo("project");
		assertThat(this.handler.lastRequest.isDetectType()).isTrue();
		assertThat(this.handler.lastRequest.getType()).isNull();
	}

	@Test
	public void parseFormatOnly() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("--format=web");
		assertThat(this.handler.lastRequest.getBuild()).isEqualTo("maven");
		assertThat(this.handler.lastRequest.getFormat()).isEqualTo("web");
		assertThat(this.handler.lastRequest.isDetectType()).isTrue();
		assertThat(this.handler.lastRequest.getType()).isNull();
	}

	@Test
	public void parseLocation() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("foobar.zip");
		assertThat(this.handler.lastRequest.getOutput()).isEqualTo("foobar.zip");
	}

	@Test
	public void parseLocationWithSlash() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("foobar/");
		assertThat(this.handler.lastRequest.getOutput()).isEqualTo("foobar");
		assertThat(this.handler.lastRequest.isExtract()).isTrue();
	}

	@Test
	public void parseMoreThanOneArg() throws Exception {
		this.handler.disableProjectGeneration();
		assertThat(this.command.run("foobar", "barfoo")).isEqualTo(ExitStatus.ERROR);
	}

	@Test
	public void userAgent() throws Exception {
		this.command.run("--list", "--target=http://fake-service");
		verify(this.http).execute(this.requestCaptor.capture());
		Header agent = this.requestCaptor.getValue().getHeaders("User-Agent")[0];
		assertThat(agent.getValue()).startsWith("SpringBootCli/");
	}

	private byte[] createFakeZipArchive(String fileName, String content)
			throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		try {
			ZipEntry entry = new ZipEntry(fileName);
			zos.putNextEntry(entry);
			zos.write(content.getBytes());
			zos.closeEntry();
		}
		finally {
			bos.close();
		}
		return bos.toByteArray();
	}

	private static class TestableInitCommandOptionHandler
			extends InitCommand.InitOptionHandler {

		private boolean disableProjectGeneration;

		private ProjectGenerationRequest lastRequest;

		TestableInitCommandOptionHandler(InitializrService initializrService) {
			super(initializrService);
		}

		void disableProjectGeneration() {
			this.disableProjectGeneration = true;
		}

		@Override
		protected void generateProject(OptionSet options) throws IOException {
			this.lastRequest = createProjectGenerationRequest(options);
			if (!this.disableProjectGeneration) {
				super.generateProject(options);
			}
		}

	}

}
