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

package org.springframework.boot.cli.command.init;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.boot.cli.util.Log;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

/**
 * Helper class used to generate the project.
 *
 * @author Stephane Nicoll
 */
class ProjectGenerator {

	private static final String ZIP_MIME_TYPE = "application/zip";

	private final InitializrService initializrService;

	ProjectGenerator(InitializrService initializrService) {
		this.initializrService = initializrService;
	}

	void generateProject(ProjectGenerationRequest request, boolean force) throws IOException {
		ProjectGenerationResponse response = this.initializrService.generate(request);
		String fileName = (request.getOutput() != null) ? request.getOutput() : response.getFileName();
		if (shouldExtract(request, response)) {
			if (isZipArchive(response)) {
				extractProject(response, request.getOutput(), force);
				return;
			}
			else {
				Log.info("Could not extract '" + response.getContentType() + "'");
				// Use value from the server since we can't extract it
				fileName = response.getFileName();
			}
		}
		if (fileName == null) {
			throw new ReportableException("Could not save the project, the server did not set a preferred "
					+ "file name and no location was set. Specify the output location for the project.");
		}
		writeProject(response, fileName, force);
	}

	/**
	 * Detect if the project should be extracted.
	 * @param request the generation request
	 * @param response the generation response
	 * @return if the project should be extracted
	 */
	private boolean shouldExtract(ProjectGenerationRequest request, ProjectGenerationResponse response) {
		if (request.isExtract()) {
			return true;
		}
		// explicit name hasn't been provided for an archive and there is no extension
		return isZipArchive(response) && request.getOutput() != null && !request.getOutput().contains(".");
	}

	private boolean isZipArchive(ProjectGenerationResponse entity) {
		if (entity.getContentType() != null) {
			try {
				return ZIP_MIME_TYPE.equals(entity.getContentType().getMimeType());
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return false;
	}

	private void extractProject(ProjectGenerationResponse entity, String output, boolean overwrite) throws IOException {
		File outputDirectory = (output != null) ? new File(output) : new File(System.getProperty("user.dir"));
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(entity.getContent()))) {
			extractFromStream(zipStream, overwrite, outputDirectory);
			fixExecutableFlag(outputDirectory, "mvnw");
			fixExecutableFlag(outputDirectory, "gradlew");
			Log.info("Project extracted to '" + outputDirectory.getAbsolutePath() + "'");
		}
	}

	private void extractFromStream(ZipInputStream zipStream, boolean overwrite, File outputDirectory)
			throws IOException {
		ZipEntry entry = zipStream.getNextEntry();
		String canonicalOutputPath = outputDirectory.getCanonicalPath() + File.separator;
		while (entry != null) {
			File file = new File(outputDirectory, entry.getName());
			String canonicalEntryPath = file.getCanonicalPath();
			if (!canonicalEntryPath.startsWith(canonicalOutputPath)) {
				throw new ReportableException("Entry '" + entry.getName() + "' would be written to '"
						+ canonicalEntryPath + "'. This is outside the output location of '" + canonicalOutputPath
						+ "'. Verify your target server configuration.");
			}
			if (file.exists() && !overwrite) {
				throw new ReportableException((file.isDirectory() ? "Directory" : "File") + " '" + file.getName()
						+ "' already exists. Use --force if you want to overwrite or "
						+ "specify an alternate location.");
			}
			if (!entry.isDirectory()) {
				FileCopyUtils.copy(StreamUtils.nonClosing(zipStream), new FileOutputStream(file));
			}
			else {
				file.mkdir();
			}
			zipStream.closeEntry();
			entry = zipStream.getNextEntry();
		}
	}

	private void writeProject(ProjectGenerationResponse entity, String output, boolean overwrite) throws IOException {
		File outputFile = new File(output);
		if (outputFile.exists()) {
			if (!overwrite) {
				throw new ReportableException(
						"File '" + outputFile.getName() + "' already exists. Use --force if you want to "
								+ "overwrite or specify an alternate location.");
			}
			if (!outputFile.delete()) {
				throw new ReportableException("Failed to delete existing file " + outputFile.getPath());
			}
		}
		FileCopyUtils.copy(entity.getContent(), outputFile);
		Log.info("Content saved to '" + output + "'");
	}

	private void fixExecutableFlag(File dir, String fileName) {
		File f = new File(dir, fileName);
		if (f.exists()) {
			f.setExecutable(true, false);
		}
	}

}
