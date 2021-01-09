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

package org.springframework.boot.context.config;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.context.config.LocationResourceLoader.ResourceType;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link LocationResourceLoader}.
 *
 * @author Phillip Webb
 */
class LocationResourceLoaderTests {

	private LocationResourceLoader loader = new LocationResourceLoader(new DefaultResourceLoader());

	@TempDir
	File temp;

	@Test
	void isPatternWhenHasAsteriskReturnsTrue() {
		assertThat(this.loader.isPattern("spring/*/boot")).isTrue();
	}

	@Test
	void isPatternWhenNoAsteriskReturnsFalse() {
		assertThat(this.loader.isPattern("spring/boot")).isFalse();
	}

	@Test
	void getResourceWhenPatternThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> this.loader.getResource("spring/boot/*"))
				.withMessage("Location 'spring/boot/*' must not be a pattern");
	}

	@Test
	void getResourceReturnsResource() throws Exception {
		File file = new File(this.temp, "file");
		FileCopyUtils.copy("test".getBytes(), file);
		Resource resource = this.loader.getResource(file.toURI().toString());
		assertThat(resource.getInputStream()).hasContent("test");
	}

	@Test
	void getResourceWhenNotUrlReturnsResource() throws Exception {
		File file = new File(this.temp, "file");
		FileCopyUtils.copy("test".getBytes(), file);
		Resource resource = this.loader.getResource(file.getAbsolutePath());
		assertThat(resource.getInputStream()).hasContent("test");
	}

	@Test
	void getResourceWhenNonCleanPathReturnsResource() throws Exception {
		File file = new File(this.temp, "file");
		FileCopyUtils.copy("test".getBytes(), file);
		Resource resource = this.loader.getResource(this.temp.getAbsolutePath() + "/spring/../file");
		assertThat(resource.getInputStream()).hasContent("test");
	}

	@Test
	void getResourcesWhenNotPatternThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> this.loader.getResources("spring/boot", ResourceType.FILE))
				.withMessage("Location 'spring/boot' must be a pattern");
	}

	@Test
	void getResourcesWhenLocationStartsWithClasspathWildcardThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.loader.getResources("classpath*:spring/boot/*/", ResourceType.FILE))
				.withMessage("Location 'classpath*:spring/boot/*/' cannot use classpath wildcards");
	}

	@Test
	void getResourcesWhenLocationContainsMultipleWildcardsThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.loader.getResources("spring/*/boot/*/", ResourceType.FILE))
				.withMessage("Location 'spring/*/boot/*/' cannot contain multiple wildcards");
	}

	@Test
	void getResourcesWhenPatternDoesNotEndWithAsteriskSlashThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> this.loader.getResources("spring/boot/*", ResourceType.FILE))
				.withMessage("Location 'spring/boot/*' must end with '*/'");
	}

	@Test
	void getFileResourceReturnsResources() throws Exception {
		createTree();
		Resource[] resources = this.loader.getResources(this.temp.getAbsolutePath() + "/*/file", ResourceType.FILE);
		assertThat(resources).hasSize(2);
		assertThat(resources[0].getInputStream()).hasContent("a");
		assertThat(resources[1].getInputStream()).hasContent("b");
	}

	@Test
	void getDirectoryResourceReturnsResources() throws Exception {
		createTree();
		Resource[] resources = this.loader.getResources(this.temp.getAbsolutePath() + "/*/", ResourceType.DIRECTORY);
		assertThat(resources).hasSize(2);
		assertThat(resources[0].getFilename()).isEqualTo("a");
		assertThat(resources[1].getFilename()).isEqualTo("b");
	}

	@Test
	void getResourcesWhenHasHiddenDirectoriesFiltersResults() throws IOException {
		createTree();
		File hiddenDirectory = new File(this.temp, "..a");
		hiddenDirectory.mkdirs();
		FileCopyUtils.copy("h".getBytes(), new File(hiddenDirectory, "file"));
		Resource[] resources = this.loader.getResources(this.temp.getAbsolutePath() + "/*/file", ResourceType.FILE);
		assertThat(resources).hasSize(2);
		assertThat(resources[0].getInputStream()).hasContent("a");
		assertThat(resources[1].getInputStream()).hasContent("b");
	}

	private void createTree() throws IOException {
		File directoryA = new File(this.temp, "a");
		File directoryB = new File(this.temp, "b");
		directoryA.mkdirs();
		directoryB.mkdirs();
		FileCopyUtils.copy("a".getBytes(), new File(directoryA, "file"));
		FileCopyUtils.copy("b".getBytes(), new File(directoryB, "file"));
	}

}
