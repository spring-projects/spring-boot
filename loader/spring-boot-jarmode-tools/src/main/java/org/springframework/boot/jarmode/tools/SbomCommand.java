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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.loader.jarmode.JarModeErrorException;
import org.springframework.util.StreamUtils;

/**
 * The {@code 'sbom'} tools command.
 *
 * @author Hyeongjun Cho
 */
class SbomCommand extends Command {

	private static final Option DESTINATION_OPTION = Option.of("destination", "string",
			"File to write the SBOM to. Defaults to printing the SBOM to the console");

	private static final String SBOM_LOCATION_ATTRIBUTE = "Sbom-Location";

	private final Context context;

	SbomCommand(Context context) {
		super("sbom", "Print the SBOM from the jar", Options.of(DESTINATION_OPTION), Parameters.none());
		this.context = context;
	}

	@Override
	void run(PrintStream out, Map<Option, @Nullable String> options, List<String> parameters) {
		try (JarFile jarFile = new JarFile(this.context.getArchiveFile())) {
			String location = getSbomLocation(jarFile);
			ZipEntry entry = jarFile.getEntry(location);
			if (entry == null || entry.isDirectory()) {
				throw new JarModeErrorException(
						"SBOM '%s' declared in the manifest was not found in the jar".formatted(location));
			}
			try (InputStream in = jarFile.getInputStream(entry)) {
				writeSbom(in, out, options);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private String getSbomLocation(JarFile jarFile) throws IOException {
		Manifest manifest = jarFile.getManifest();
		if (manifest != null) {
			String location = manifest.getMainAttributes().getValue(SBOM_LOCATION_ATTRIBUTE);
			if (location != null) {
				return location;
			}
		}
		throw new JarModeErrorException(
				"No SBOM found in the jar; the manifest has no '%s' attribute".formatted(SBOM_LOCATION_ATTRIBUTE));
	}

	private void writeSbom(InputStream in, PrintStream out, Map<Option, @Nullable String> options) throws IOException {
		String destination = options.get(DESTINATION_OPTION);
		if (destination == null) {
			StreamUtils.copy(in, out);
			if (out.checkError()) {
				throw new JarModeErrorException("Failed to write the SBOM to the console");
			}
			return;
		}
		File file = getDestinationFile(destination);
		if (file.isDirectory()) {
			throw new JarModeErrorException(file.getAbsoluteFile() + " already exists and is a directory");
		}
		mkdirs(file.getParentFile());
		try (OutputStream fileOut = new FileOutputStream(file)) {
			StreamUtils.copy(in, fileOut);
		}
	}

	private File getDestinationFile(String destination) {
		File file = new File(destination);
		if (file.isAbsolute()) {
			return file;
		}
		return new File(this.context.getWorkingDir(), file.getPath());
	}

	private static void mkdirs(@Nullable File file) throws IOException {
		if (file != null && !file.exists() && !file.mkdirs()) {
			throw new IOException("Unable to create directory " + file);
		}
	}

}
