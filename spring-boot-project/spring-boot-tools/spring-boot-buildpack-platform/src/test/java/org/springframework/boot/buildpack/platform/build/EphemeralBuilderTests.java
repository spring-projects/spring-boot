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

package org.springframework.boot.buildpack.platform.build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.docker.type.ImageConfig;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EphemeralBuilder}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class EphemeralBuilderTests extends AbstractJsonTests {

	@TempDir
	File temp;

	private final BuildOwner owner = BuildOwner.of(123, 456);

	private Image image;

	private BuilderMetadata metadata;

	private Map<String, String> env;

	private Creator creator = Creator.withVersion("dev");

	@BeforeEach
	void setup() throws Exception {
		this.image = Image.of(getContent("image.json"));
		this.metadata = BuilderMetadata.fromImage(this.image);
		this.env = Collections.singletonMap("spring", "boot");
	}

	@Test
	void getNameHasRandomName() throws Exception {
		EphemeralBuilder b1 = new EphemeralBuilder(this.owner, this.image, this.metadata, this.creator, this.env);
		EphemeralBuilder b2 = new EphemeralBuilder(this.owner, this.image, this.metadata, this.creator, this.env);
		assertThat(b1.getName().toString()).startsWith("pack.local/builder/").endsWith(":latest");
		assertThat(b1.getName().toString()).isNotEqualTo(b2.getName().toString());
	}

	@Test
	void getArchiveHasCreatedByConfig() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.creator, this.env);
		ImageConfig config = builder.getArchive().getImageConfig();
		BuilderMetadata ephemeralMetadata = BuilderMetadata.fromImageConfig(config);
		assertThat(ephemeralMetadata.getCreatedBy().getName()).isEqualTo("Spring Boot");
		assertThat(ephemeralMetadata.getCreatedBy().getVersion()).isEqualTo("dev");
	}

	@Test
	void getArchiveHasTag() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.creator, this.env);
		ImageReference tag = builder.getArchive().getTag();
		assertThat(tag.toString()).startsWith("pack.local/builder/").endsWith(":latest");
	}

	@Test
	void getArchiveHasFixedCreateDate() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.creator, this.env);
		Instant createInstant = builder.getArchive().getCreateDate();
		OffsetDateTime createDateTime = OffsetDateTime.ofInstant(createInstant, ZoneId.of("UTC"));
		assertThat(createDateTime.getYear()).isEqualTo(1980);
		assertThat(createDateTime.getMonthValue()).isEqualTo(1);
		assertThat(createDateTime.getDayOfMonth()).isEqualTo(1);
		assertThat(createDateTime.getHour()).isEqualTo(0);
		assertThat(createDateTime.getMinute()).isEqualTo(0);
		assertThat(createDateTime.getSecond()).isEqualTo(1);
	}

	@Test
	void getArchiveContainsEnvLayer() throws Exception {
		EphemeralBuilder builder = new EphemeralBuilder(this.owner, this.image, this.metadata, this.creator, this.env);
		File directory = unpack(getLayer(builder.getArchive(), 0), "env");
		assertThat(new File(directory, "platform/env/spring")).usingCharset(StandardCharsets.UTF_8).hasContent("boot");
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
		File directory = new File(this.temp, name);
		directory.mkdirs();
		ArchiveEntry entry = archive.getNextEntry();
		while (entry != null) {
			File file = new File(directory, entry.getName());
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
		return directory;
	}

}
