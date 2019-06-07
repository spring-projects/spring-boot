/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;

/**
 * {@link AbstractApplicationLauncher} that launches a Spring Boot application using
 * {@code JarLauncher} or {@code WarLauncher} and an exploded archive.
 *
 * @author Andy Wilkinson
 */
class ExplodedApplicationLauncher extends AbstractApplicationLauncher {

	private final File exploded = new File("target/exploded");

	ExplodedApplicationLauncher(ApplicationBuilder applicationBuilder) {
		super(applicationBuilder);
	}

	@Override
	protected File getWorkingDirectory() {
		return this.exploded;
	}

	@Override
	protected String getDescription(String packaging) {
		return "exploded " + packaging;
	}

	@Override
	protected List<String> getArguments(File archive) {
		String mainClass = (archive.getName().endsWith(".war") ? "org.springframework.boot.loader.WarLauncher"
				: "org.springframework.boot.loader.JarLauncher");
		try {
			explodeArchive(archive);
			return Arrays.asList("-cp", this.exploded.getAbsolutePath(), mainClass);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void explodeArchive(File archive) throws IOException {
		FileSystemUtils.deleteRecursively(this.exploded);
		JarFile jarFile = new JarFile(archive);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			File extracted = new File(this.exploded, jarEntry.getName());
			if (jarEntry.isDirectory()) {
				extracted.mkdirs();
			}
			else {
				FileOutputStream extractedOutputStream = new FileOutputStream(extracted);
				StreamUtils.copy(jarFile.getInputStream(jarEntry), extractedOutputStream);
				extractedOutputStream.close();
			}
		}
		jarFile.close();
	}

}
