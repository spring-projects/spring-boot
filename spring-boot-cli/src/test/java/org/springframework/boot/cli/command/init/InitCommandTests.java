/*
 * Copyright 2012-2014 the original author or authors.
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

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.cli.command.status.ExitStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link InitCommand}
 *
 * @author Stephane Nicoll
 */
public class InitCommandTests extends AbstractHttpClientMockTests {

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	private final TestableInitCommandOptionHandler handler = new TestableInitCommandOptionHandler(
			this.httpClient);

	private final InitCommand command = new InitCommand(this.handler);

	@Test
	public void listServiceCapabilities() throws Exception {
		mockSuccessfulMetadataGet();
		this.command.run("--list", "--target=http://fake-service");
	}

	@Test
	public void generateProject() throws Exception {
		String fileName = UUID.randomUUID().toString() + ".zip";
		File f = new File(fileName);
		assertFalse("file should not exist", f.exists());

		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", fileName);
		mockSuccessfulProjectGeneration(mockHttpRequest);

		try {
			assertEquals(ExitStatus.OK, this.command.run());
			assertTrue("file should have been created", f.exists());
		}
		finally {
			assertTrue("failed to delete test file", f.delete());
		}
	}

	@Test
	public void generateProjectNoFileNameAvailable() throws Exception {
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", null);
		mockSuccessfulProjectGeneration(mockHttpRequest);
		assertEquals(ExitStatus.ERROR, this.command.run());
	}

	@Test
	public void generateProjectAndExtract() throws Exception {
		File f = this.folder.newFolder();

		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(mockHttpRequest);

		assertEquals(ExitStatus.OK,
				this.command.run("--extract", "--output=" + f.getAbsolutePath()));
		File archiveFile = new File(f, "test.txt");
		assertTrue(
				"Archive not extracted properly " + f.getAbsolutePath() + " not found",
				archiveFile.exists());
	}

	@Test
	public void generateProjectAndExtractUnsupportedArchive() throws Exception {
		File f = this.folder.newFolder();
		String fileName = UUID.randomUUID().toString() + ".zip";
		File archiveFile = new File(fileName);
		assertFalse("file should not exist", archiveFile.exists());

		try {
			byte[] archive = createFakeZipArchive("test.txt", "Fake content");
			MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
					"application/foobar", fileName, archive);
			mockSuccessfulProjectGeneration(mockHttpRequest);

			assertEquals(ExitStatus.OK,
					this.command.run("--extract", "--output=" + f.getAbsolutePath()));
			assertTrue("file should have been saved instead", archiveFile.exists());
		}
		finally {
			assertTrue("failed to delete test file", archiveFile.delete());
		}
	}

	@Test
	public void generateProjectAndExtractUnknownContentType() throws Exception {
		File f = this.folder.newFolder();
		String fileName = UUID.randomUUID().toString() + ".zip";
		File archiveFile = new File(fileName);
		assertFalse("file should not exist", archiveFile.exists());

		try {
			byte[] archive = createFakeZipArchive("test.txt", "Fake content");
			MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
					null, fileName, archive);
			mockSuccessfulProjectGeneration(mockHttpRequest);

			assertEquals(ExitStatus.OK,
					this.command.run("--extract", "--output=" + f.getAbsolutePath()));
			assertTrue("file should have been saved instead", archiveFile.exists());
		}
		finally {
			assertTrue("failed to delete test file", archiveFile.delete());
		}
	}

	@Test
	public void fileNotOverwrittenByDefault() throws Exception {
		File f = this.folder.newFile();
		long fileLength = f.length();

		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", f.getAbsolutePath());
		mockSuccessfulProjectGeneration(mockHttpRequest);

		assertEquals("Should have failed", ExitStatus.ERROR, this.command.run());
		assertEquals("File should not have changed", fileLength, f.length());
	}

	@Test
	public void overwriteFile() throws Exception {
		File f = this.folder.newFile();
		long fileLength = f.length();

		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", f.getAbsolutePath());
		mockSuccessfulProjectGeneration(mockHttpRequest);
		assertEquals("Should not have failed", ExitStatus.OK, this.command.run("--force"));
		assertTrue("File should have changed", fileLength != f.length());
	}

	@Test
	public void fileInArchiveNotOverwrittenByDefault() throws Exception {
		File f = this.folder.newFolder();
		File conflict = new File(f, "test.txt");
		assertTrue("Should have been able to create file", conflict.createNewFile());
		long fileLength = conflict.length();

		// also contains test.txt
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(mockHttpRequest);

		assertEquals(ExitStatus.ERROR,
				this.command.run("--extract", "--output=" + f.getAbsolutePath()));
		assertEquals("File should not have changed", fileLength, conflict.length());
	}

	@Test
	public void overwriteFileInArchive() throws Exception {
		File f = this.folder.newFolder();
		File conflict = new File(f, "test.txt");
		assertTrue("Should have been able to create file", conflict.createNewFile());
		long fileLength = conflict.length();

		// also contains test.txt
		byte[] archive = createFakeZipArchive("test.txt", "Fake content");
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/zip", "demo.zip", archive);
		mockSuccessfulProjectGeneration(mockHttpRequest);

		assertEquals(
				ExitStatus.OK,
				this.command.run("--force", "--extract",
						"--output=" + f.getAbsolutePath()));
		assertTrue("File should have changed", fileLength != conflict.length());
	}

	@Test
	public void parseProjectOptions() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("-bv=1.2.0.RELEASE", "-d=web,data-jpa", "-jv=1.9", "-p=war",
				"--build=grunt", "--format=web", "-t=ant-project");

		assertEquals("1.2.0.RELEASE", this.handler.lastRequest.getBootVersion());
		List<String> dependencies = this.handler.lastRequest.getDependencies();
		assertEquals(2, dependencies.size());
		assertTrue(dependencies.contains("web"));
		assertTrue(dependencies.contains("data-jpa"));
		assertEquals("1.9", this.handler.lastRequest.getJavaVersion());
		assertEquals("war", this.handler.lastRequest.getPackaging());
		assertEquals("grunt", this.handler.lastRequest.getBuild());
		assertEquals("web", this.handler.lastRequest.getFormat());
		assertEquals("ant-project", this.handler.lastRequest.getType());
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
	public void parseOutput() throws Exception {
		this.handler.disableProjectGeneration();
		this.command.run("--output=foobar.zip");

		assertEquals("foobar.zip", this.handler.lastRequest.getOutput());
	}

	private byte[] createFakeZipArchive(String fileName, String content)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(out);
		try {
			ZipEntry entry = new ZipEntry(fileName);

			zos.putNextEntry(entry);
			zos.write(content.getBytes());
			zos.closeEntry();
		}
		finally {
			out.close();
		}
		return out.toByteArray();
	}

	private static class TestableInitCommandOptionHandler extends
			InitCommandOptionHandler {

		private boolean disableProjectGeneration;

		ProjectGenerationRequest lastRequest;

		TestableInitCommandOptionHandler(CloseableHttpClient httpClient) {
			super(httpClient);
		}

		void disableProjectGeneration() {
			this.disableProjectGeneration = true;
		}

		@Override
		protected ExitStatus generateProject(OptionSet options,
				CloseableHttpClient httpClient) {
			this.lastRequest = createProjectGenerationRequest(options);
			if (!this.disableProjectGeneration) {
				return super.generateProject(options, httpClient);
			}
			return ExitStatus.OK;
		}
	}

}
