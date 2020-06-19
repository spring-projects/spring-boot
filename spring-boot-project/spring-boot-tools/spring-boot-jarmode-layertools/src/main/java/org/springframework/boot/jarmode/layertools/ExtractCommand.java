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

package org.springframework.boot.jarmode.layertools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * The {@code 'extract'} tools command.
 *
 * @author Phillip Webb
 */
class ExtractCommand extends Command {

	static final Option DESTINATION_OPTION = Option.of("destination", "string", "The destination to extract files to");

	private final Context context;

	private final Layers layers;

	ExtractCommand(Context context) {
		this(context, Layers.get(context));
	}

	ExtractCommand(Context context, Layers layers) {
		super("extract", "Extracts layers from the jar for image creation", Options.of(DESTINATION_OPTION),
				Parameters.of("[<layer>...]"));
		this.context = context;
		this.layers = layers;
	}

	@Override
	protected void run(Map<Option, String> options, List<String> parameters) {
		try {
			File destination = options.containsKey(DESTINATION_OPTION) ? new File(options.get(DESTINATION_OPTION))
					: this.context.getWorkingDir();
			for (String layer : this.layers) {
				if (parameters.isEmpty() || parameters.contains(layer)) {
					mkDirs(new File(destination, layer));
				}
			}
			try (ZipInputStream zip = new ZipInputStream(new FileInputStream(this.context.getJarFile()))) {
				ZipEntry entry = zip.getNextEntry();
				while (entry != null) {
					if (!entry.isDirectory()) {
						String layer = this.layers.getLayer(entry);
						if (parameters.isEmpty() || parameters.contains(layer)) {
							write(zip, entry, new File(destination, layer));
						}
					}
					entry = zip.getNextEntry();
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void write(ZipInputStream zip, ZipEntry entry, File destination) throws IOException {
		String path = StringUtils.cleanPath(entry.getName());
		File file = new File(destination, path);
		if (file.getAbsolutePath().startsWith(destination.getAbsolutePath())) {
			mkParentDirs(file);
			try (OutputStream out = new FileOutputStream(file)) {
				StreamUtils.copy(zip, out);
			}
			Files.setAttribute(file.toPath(), "creationTime", entry.getCreationTime());
		}
	}

	private void mkParentDirs(File file) throws IOException {
		mkDirs(file.getParentFile());
	}

	private void mkDirs(File file) throws IOException {
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("Unable to create directory " + file);
		}
	}

}
