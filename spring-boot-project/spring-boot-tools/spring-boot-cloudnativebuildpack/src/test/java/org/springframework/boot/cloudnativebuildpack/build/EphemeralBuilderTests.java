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

package org.springframework.boot.cloudnativebuildpack.build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.cloudnativebuildpack.docker.type.Image;
import org.springframework.boot.cloudnativebuildpack.docker.type.ImageArchive;
import org.springframework.boot.cloudnativebuildpack.docker.type.ImageConfig;
import org.springframework.boot.cloudnativebuildpack.docker.type.ImageReference;
import org.springframework.boot.cloudnativebuildpack.json.AbstractJsonTests;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EphemeralBuilder}.
 *
 * @author Phillip Webb
 */
class EphemeralBuilderTests extends AbstractJsonTests {

	@TempDir
	File temp;

	private final BuildOwner owner = BuildOwner.of(123, 456);

	private Image image;

	private BuilderMetadata metadata;

	private Map<String, String> env;

	@BeforeEach
	void setup() throws Exception {
		this.image = Image.of(getContent("image.json"));
		this.metadata = BuilderMetadata.fromImage(this.image);
		this.env = Collections.singletonMap("spring", "boot");
	}

	@Test
	void getNameHasRandomName() throws Exception {
		EphemeralBuilder b1 = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		EphemeralBuilder b2 = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		assertThat(b1.getName().toString()).startsWith("pack.local/builder/").endsWith(":latest");
		assertThat(b1.getName().toString()).isNotEqualTo(b2.getName().toString());
	}

	@Test
	void getArchiveHasCreatedByConfig() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		ImageConfig config = builder.getArchive().getImageConfig();
		BuilderMetadata ephemeralMetadata = BuilderMetadata.fromImageConfig(config);
		assertThat(ephemeralMetadata.getCreatedBy().getName()).isEqualTo("Spring Boot");
	}

	@Test
	void getArchiveHasTag() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		ImageReference tag = builder.getArchive().getTag();
		assertThat(tag.toString()).startsWith("pack.local/builder/").endsWith(":latest");
	}

	@Test
	void getArchiveHasCreateDate() throws Exception {
		Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
		EphemeralBuilder builder = new EphemeralBuilder(clock, this.owner, this.image, this.metadata, this.env);
		assertThat(builder.getArchive().getCreateDate()).isEqualTo(Instant.now(clock));
	}

	@Test
	void getArchiveContainsDefaultDirsLayer() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		File folder = unpack(getLayer(builder.getArchive(), 0), "dirs");
		assertThat(new File(folder, "workspace")).isDirectory();
		assertThat(new File(folder, "layers")).isDirectory();
		assertThat(new File(folder, "cnb")).isDirectory();
		assertThat(new File(folder, "cnb/buildpacks")).isDirectory();
		assertThat(new File(folder, "platform")).isDirectory();
		assertThat(new File(folder, "platform/env")).isDirectory();
	}

	@Test
	void getArchiveContainsStackLayer() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		File folder = unpack(getLayer(builder.getArchive(), 1), "stack");
		File tomlFile = new File(folder, "cnb/stack.toml");
		assertThat(tomlFile).exists();
		String toml = FileCopyUtils
				.copyToString(new InputStreamReader(new FileInputStream(tomlFile), StandardCharsets.UTF_8));
		assertThat(toml).contains("[run-image]").contains("image = ");
	}

	@Test
	void getArchiveContainsEnvLayer() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.env);
		File folder = unpack(getLayer(builder.getArchive(), 2), "env");
		assertThat(new File(folder, "platform/env/spring")).usingCharset(StandardCharsets.UTF_8).hasContent("boot");
	}

	private TarArchiveInputStream getLayer(ImageArchive archive, int index) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		archive.writeTo(outputStream);
		TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
		for (int i = 0; i <= index; i++) {
			tar.getNextEntry();
		}
		return new TarArchiveInputStream(tar);
	}

	private File unpack(TarArchiveInputStream archive, String name) throws Exception {
		File folder = new File(this.temp, name);
		folder.mkdirs();
		ArchiveEntry entry = archive.getNextEntry();
		while (entry != null) {
			File file = new File(folder, entry.getName());
			if (entry.isDirectory()) {
				file.mkdirs();
			}
			else {
				file.getParentFile().mkdirs();
				try (OutputStream out = new FileOutputStream(file)) {
					IOUtils.copy(archive, out);
				}
			}
			entry = archive.getNextEntry();
		}
		return folder;
	}

}
