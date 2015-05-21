/*
 * Copyright 2012-2015 the original author or authors.
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

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
		assertFalse("file should not exist", file.exists());
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", fileName);
		mockSuccessfulProjectGeneration(request);
		try {
			assertEquals(ExitStatus.OK, this.command.run());
			assertTrue("file should have been created", file.exists());
		}
		finally {
			assertTrue("failed to delete test file", file.delete());
		}
	}

	@Test
	public void generateProjectNoFileNameAvailable() throws Exception {
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", null);
		mockSuccessfulProjectGeneration(request);
		assertEquals(ExitStatus.ERROR, this.command.run());
	}

	@Test
	public void generateProjectAndExtract() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertEquals(ExitStatus.OK,
				this.command.run("--extract", folder.getAbsolutePath()));
		File archiveFile = new File(folder, "test.txt");
		assertTrue("Archive not extracted properly " + archiveFile.getAbsolutePath()
				+ " not found", archiveFile.exists());
	}

	@Test
	public void generateProjectAndExtractWithConvention() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertEquals(ExitStatus.OK, this.command.run(folder.getAbsolutePath() + "/"));
		File archiveFile = new File(folder, "test.txt");
		assertTrue("Archive not extracted properly " + archiveFile.getAbsolutePath()
				+ " not found", archiveFile.exists());
	}

	@Test
	public void generateProjectArchiveExtractedByDefault() throws Exception {
		String fileName = UUID.randomUUID().toString();
		assertFalse("No dot in filename", fileName.contains("."));
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		File file = new File(fileName);
		File archiveFile = new File(file, "test.txt");
		try {
			assertEquals(ExitStatus.OK, this.command.run(fileName));
			assertTrue("Archive not extracted properly " + archiveFile.getAbsolutePath()
					+ " not found", archiveFile.exists());
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
			assertEquals(ExitStatus.OK, this.command.run(fileName));
			assertTrue("File not saved properly", file.exists());
			assertTrue("Should not be a directory", file.isFile());
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
		assertFalse("file should not exist", file.exists());
		try {
			byte[] archive = createFakeZipArchive("test.txt", "Fake content");
			MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
					"application/foobar", fileName, archive);
			mockSuccessfulProjectGeneration(request);
			assertEquals(ExitStatus.OK,
					this.command.run("--extract", folder.getAbsolutePath()));
			assertTrue("file should have been saved instead", file.exists());
		}
		finally {
			assertTrue("failed to delete test file", file.delete());
		}
	}

	@Test
	public void generateProjectAndExtractUnknownContentType() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		String fileName = UUID.randomUUID().toString() + ".zip";
		File file = new File(fileName);
		assertFalse("file should not exist", file.exists());
		try {
			byte[] archive = createFakeZipArchive("test.txt", "Fake content");
			MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
					null, fileName, archive);
			mockSuccessfulProjectGeneration(request);
			assertEquals(ExitStatus.OK,
					this.command.run("--extract", folder.getAbsolutePath()));
			assertTrue("file should have been saved instead", file.exists());
		}
		finally {
			assertTrue("failed to delete test file", file.delete());
		}
	}

	@Test
	public void fileNotOverwrittenByDefault() throws Exception {
		File file = this.temporaryFolder.newFile();
		long fileLength = file.length();
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", file.getAbsolutePath());
		mockSuccessfulProjectGeneration(request);
		assertEquals("Should have failed", ExitStatus.ERROR, this.command.run());
		assertEquals("File should not have changed", fileLength, file.length());
	}

	@Test
	public void overwriteFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		long fileLength = file.length();
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", file.getAbsolutePath());
		mockSuccessfulProjectGeneration(request);
		assertEquals("Should not have failed", ExitStatus.OK, this.command.run("--force"));
		assertTrue("File should have changed", fileLength != file.length());
	}

	@Test
	public void fileInArchiveNotOverwrittenByDefault() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		File conflict = new File(folder, "test.txt");
		assertTrue("Should have been able to create file", conflict.createNewFile());
		long fileLength = conflict.length();
		// also contains test.txt
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertEquals(ExitStatus.ERROR,
				this.command.run("--extract", folder.getAbsolutePath()));
		assertEquals("File should not have changed", fileLength, conflict.length());
	}

	@Test
	public void parseProjectOptions() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("-g=org.demo", "-a=acme", "-v=1.2.3-SNAPSHOT", "-n=acme-sample",
				"--description=Acme sample project", "-p=war", "-t=ant-project",
				"--build=grunt", "--format=web", "-j=1.9", "-l=groovy",
				"-b=1.2.0.RELEASE", "-d=web,data-jpa");
		assertEquals("org.demo", this.handler.lastRequest.getGroupId());
		assertEquals("acme", this.handler.lastRequest.getArtifactId());
		assertEquals("1.2.3-SNAPSHOT", this.handler.lastRequest.getVersion());
		assertEquals("acme-sample", this.handler.lastRequest.getName());
		assertEquals("Acme sample project", this.handler.lastRequest.getDescription());
		assertEquals("war", this.handler.lastRequest.getPackaging());
		assertEquals("ant-project", this.handler.lastRequest.getType());
		assertEquals("grunt", this.handler.lastRequest.getBuild());
		assertEquals("web", this.handler.lastRequest.getFormat());
		assertEquals("1.9", this.handler.lastRequest.getJavaVersion());
		assertEquals("groovy", this.handler.lastRequest.getLanguage());
		assertEquals("1.2.0.RELEASE", this.handler.lastRequest.getBootVersion());
		List<String> dependencies = this.handler.lastRequest.getDependencies();
		assertEquals(2, dependencies.size());
		assertTrue(dependencies.contains("web"));
		assertTrue(dependencies.contains("data-jpa"));
	}

	@Test
	public void overwriteFileInArchive() throws Exception {
		File folder = this.temporaryFolder.newFolder();
		File conflict = new File(folder, "test.txt");
		assertTrue("Should have been able to create file", conflict.createNewFile());
		long fileLength = conflict.length();
		// also contains test.txt
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest request = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(request);
		assertEquals(ExitStatus.OK,
				this.command.run("--force", "--extract", folder.getAbsolutePath()));
		assertTrue("File should have changed", fileLength != conflict.length());
	}

	@Test
	public void parseTypeOnly() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("-t=ant-project");
		assertEquals("maven", this.handler.lastRequest.getBuild());
		assertEquals("project", this.handler.lastRequest.getFormat());
		assertFalse(this.handler.lastRequest.isDetectType());
		assertEquals("ant-project", this.handler.lastRequest.getType());
	}

	@Test
	public void parseBuildOnly() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("--build=ant");
		assertEquals("ant", this.handler.lastRequest.getBuild());
		assertEquals("project", this.handler.lastRequest.getFormat());
		assertTrue(this.handler.lastRequest.isDetectType());
		assertNull(this.handler.lastRequest.getType());
	}

	@Test
	public void parseFormatOnly() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("--format=web");
		assertEquals("maven", this.handler.lastRequest.getBuild());
		assertEquals("web", this.handler.lastRequest.getFormat());
		assertTrue(this.handler.lastRequest.isDetectType());
		assertNull(this.handler.lastRequest.getType());
	}

	@Test
	public void parseLocation() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("foobar.zip");
		assertEquals("foobar.zip", this.handler.lastRequest.getOutput());
	}

	@Test
	public void parseLocationWithSlash() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("foobar/");
		assertEquals("foobar", this.handler.lastRequest.getOutput());
		assertTrue(this.handler.lastRequest.isExtract());
	}

	@Test
	public void parseMoreThanOneArg() throws Exception {
		this.handler.disableProjectGeneration();
		assertEquals(ExitStatus.ERROR, this.command.run("foobar", "barfoo"));
	}

	@Test
	public void userAgent() throws Exception {
		this.command.run("--list", "--target=http://fake-service");
		verify(this.http).execute(this.requestCaptor.capture());
		Header agent = this.requestCaptor.getValue().getHeaders("User-Agent")[0];
		assertThat(agent.getValue(), startsWith("SpringBootCli/"));
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

	private static class TestableInitCommandOptionHandler extends
			InitCommand.InitOptionHandler {

		private boolean disableProjectGeneration;

		private ProjectGenerationRequest lastRequest;

		public TestableInitCommandOptionHandler(InitializrService initializrService) {
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
