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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.cli.command.options.OptionHandler;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.boot.cli.util.Log;
import org.springframework.util.StreamUtils;

/**
 * The {@link OptionHandler} implementation for the init command.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class InitCommandOptionHandler extends OptionHandler {

	private final CloseableHttpClient httpClient;

	private OptionSpec<String> target;

	private OptionSpec<Void> listMetadata;

	// Project generation options

	private OptionSpec<String> bootVersion;

	private OptionSpec<String> dependencies;

	private OptionSpec<String> javaVersion;

	private OptionSpec<String> packaging;

	private OptionSpec<String> build;

	private OptionSpec<String> format;

	private OptionSpec<String> type;

	// Other options

	private OptionSpec<Void> extract;

	private OptionSpec<Void> force;

	private OptionSpec<String> output;

	InitCommandOptionHandler(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	protected void options() {
		this.target = option(Arrays.asList("target"), "URL of the service to use")
				.withRequiredArg().defaultsTo(
						ProjectGenerationRequest.DEFAULT_SERVICE_URL);
		this.listMetadata = option(Arrays.asList("list", "l"),
				"List the capabilities of the service. Use it to "
						+ "discover the dependencies and the types that are available.");

		// Project generation settings
		this.bootVersion = option(Arrays.asList("boot-version", "bv"),
				"Spring Boot version to use (e.g. 1.2.0.RELEASE)").withRequiredArg();
		this.dependencies = option(Arrays.asList("dependencies", "d"),
				"Comma separated list of dependencies to include in the generated project")
				.withRequiredArg();
		this.javaVersion = option(Arrays.asList("java-version", "jv"),
				"Java version to use (e.g. 1.8)").withRequiredArg();
		this.packaging = option(Arrays.asList("packaging", "p"),
				"Packaging type to use (e.g. jar)").withRequiredArg();

		this.build = option(
				"build",
				"The build system to use (e.g. maven, gradle). To be used alongside "
						+ "--format to uniquely identify one type that is supported by the service. "
						+ "Use --type in case of conflict").withRequiredArg().defaultsTo(
				"maven");
		this.format = option(
				"format",
				"The format of the generated content (e.g. build for a build file, "
						+ "project for a project archive). To be used alongside --build to uniquely identify one type "
						+ "that is supported by the service. Use --type in case of conflict")
				.withRequiredArg().defaultsTo("project");
		this.type = option(
				Arrays.asList("type", "t"),
				"The project type to use. Not normally needed if you "
						+ "use --build and/or --format. Check the capabilities of the service (--list) for "
						+ "more details.").withRequiredArg();

		// Others
		this.extract = option(Arrays.asList("extract", "x"),
				"Extract the project archive");
		this.force = option(Arrays.asList("force", "f"),
				"Force overwrite of existing files");
		this.output = option(
				Arrays.asList("output", "o"),
				"Location of the generated project. Can be an absolute or a relative reference and "
						+ "should refer to a directory when --extract is used.")
				.withRequiredArg();
	}

	@Override
	protected ExitStatus run(OptionSet options) throws Exception {
		if (options.has(this.listMetadata)) {
			return listServiceCapabilities(options, this.httpClient);
		}
		else {
			return generateProject(options, this.httpClient);
		}
	}

	public ProjectGenerationRequest createProjectGenerationRequest(OptionSet options) {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		request.setServiceUrl(determineServiceUrl(options));

		if (options.has(this.bootVersion)) {
			request.setBootVersion(options.valueOf(this.bootVersion));
		}
		if (options.has(this.dependencies)) {
			for (String dep : options.valueOf(this.dependencies).split(",")) {
				request.getDependencies().add(dep.trim());
			}
		}
		if (options.has(this.javaVersion)) {
			request.setJavaVersion(options.valueOf(this.javaVersion));
		}
		if (options.has(this.packaging)) {
			request.setPackaging(options.valueOf(this.packaging));
		}
		request.setBuild(options.valueOf(this.build));
		request.setFormat(options.valueOf(this.format));
		request.setDetectType(options.has(this.build) || options.has(this.format));
		if (options.has(this.type)) {
			request.setType(options.valueOf(this.type));
		}
		if (options.has(this.output)) {
			request.setOutput(options.valueOf(this.output));
		}
		return request;
	}

	protected ExitStatus listServiceCapabilities(OptionSet options,
			CloseableHttpClient httpClient) throws IOException {
		ListMetadataCommand command = new ListMetadataCommand(httpClient);
		Log.info(command.generateReport(determineServiceUrl(options)));
		return ExitStatus.OK;
	}

	protected ExitStatus generateProject(OptionSet options, CloseableHttpClient httpClient) {
		ProjectGenerationRequest request = createProjectGenerationRequest(options);
		boolean forceValue = options.has(this.force);
		try {
			ProjectGenerationResponse entity = new InitializrServiceHttpInvoker(
					httpClient).generate(request);
			if (options.has(this.extract)) {
				if (isZipArchive(entity)) {
					return extractProject(entity, options.valueOf(this.output),
							forceValue);
				}
				else {
					Log.info("Could not extract '" + entity.getContentType() + "'");
				}
			}
			String outputFileName = entity.getFileName() != null ? entity.getFileName()
					: options.valueOf(this.output);
			if (outputFileName == null) {
				Log.error("Could not save the project, the server did not set a preferred "
						+ "file name. Use --output to specify the output location for the project.");
				return ExitStatus.ERROR;
			}
			return writeProject(entity, outputFileName, forceValue);
		}
		catch (ProjectGenerationException ex) {
			Log.error(ex.getMessage());
			return ExitStatus.ERROR;
		}
		catch (Exception ex) {
			Log.error(ex);
			return ExitStatus.ERROR;
		}
	}

	private String determineServiceUrl(OptionSet options) {
		return options.valueOf(this.target);
	}

	private ExitStatus writeProject(ProjectGenerationResponse entity,
			String outputFileName, boolean overwrite) throws IOException {

		File f = new File(outputFileName);
		if (f.exists()) {
			if (overwrite) {
				if (!f.delete()) {
					throw new IllegalStateException("Failed to delete existing file "
							+ f.getPath());
				}
			}
			else {
				Log.error("File '" + f.getName()
						+ "' already exists. Use --force if you want to "
						+ "overwrite or --output to specify an alternate location.");
				return ExitStatus.ERROR;
			}
		}
		FileOutputStream stream = new FileOutputStream(f);
		try {
			StreamUtils.copy(entity.getContent(), stream);
			Log.info("Content saved to '" + outputFileName + "'");
			return ExitStatus.OK;
		}
		finally {
			stream.close();
		}
	}

	private boolean isZipArchive(ProjectGenerationResponse entity) {
		if (entity.getContentType() == null) {
			return false;
		}
		try {
			return "application/zip".equals(entity.getContentType().getMimeType());
		}
		catch (Exception e) {
			return false;
		}
	}

	private ExitStatus extractProject(ProjectGenerationResponse entity,
			String outputValue, boolean overwrite) throws IOException {
		File output = outputValue != null ? new File(outputValue) : new File(
				System.getProperty("user.dir"));
		if (!output.exists()) {
			output.mkdirs();
		}
		ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(
				entity.getContent()));
		try {
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				File f = new File(output, entry.getName());
				if (f.exists() && !overwrite) {
					StringBuilder sb = new StringBuilder();
					sb.append(f.isDirectory() ? "Directory" : "File")
							.append(" '")
							.append(f.getName())
							.append("' already exists. Use --force if you want to "
									+ "overwrite or --output to specify an alternate location.");
					Log.error(sb.toString());
					return ExitStatus.ERROR;
				}
				if (!entry.isDirectory()) {
					extractZipEntry(zipIn, f);
				}
				else {
					f.mkdir();
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			Log.info("Project extracted to '" + output.getAbsolutePath() + "'");
			return ExitStatus.OK;
		}
		finally {
			zipIn.close();
		}
	}

	private void extractZipEntry(ZipInputStream in, File outputFile) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(
				outputFile));
		try {
			StreamUtils.copy(in, out);
		}
		finally {
			out.close();
		}
	}

}
