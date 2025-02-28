/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testsupport.classpath.resources;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Resources}.
 *
 * @author Andy Wilkinson
 */
class ResourcesTests {

	@TempDir
	private Path root;

	@Test
	void whenAddResourceThenResourceIsCreated() {
		new Resources(this.root).addResource("test", "test-content");
		assertThat(this.root.resolve("test")).hasContent("test-content");
	}

	@Test
	void whenAddResourceHasContentReferencingResourceRootThenResourceIsCreatedWithReferenceToRoot() {
		new Resources(this.root).addResource("test", "*** ${resourceRoot} ***");
		assertThat(this.root.resolve("test")).hasContent("*** " + this.root + " ***");
	}

	@Test
	void whenAddResourceWithPathThenResourceIsCreated() {
		new Resources(this.root).addResource("a/b/c/test", "test-content");
		assertThat(this.root.resolve("a/b/c/test")).hasContent("test-content");
	}

	@Test
	void whenAddResourceAndResourceAlreadyExistsThenResourcesIsOverwritten() {
		Resources resources = new Resources(this.root);
		resources.addResource("a/b/c/test", "original-content");
		resources.addResource("a/b/c/test", "new-content");
		assertThat(this.root.resolve("a/b/c/test")).hasContent("new-content");
	}

	@Test
	void whenAddPackageThenNamedResourcesFromPackageAreCreated() {
		new Resources(this.root).addPackage(getClass().getPackage().getName(),
				new String[] { "resource-1.txt", "sub/resource-3.txt" });
		assertThat(this.root.resolve("resource-1.txt")).hasContent("one");
		assertThat(this.root.resolve("resource-2.txt")).doesNotExist();
		assertThat(this.root.resolve("sub/resource-3.txt")).hasContent("three");
	}

	@Test
	void whenAddResourceAndDeleteThenResourceDoesNotExist() {
		Resources resources = new Resources(this.root);
		resources.addResource("test", "test-content");
		assertThat(this.root.resolve("test")).hasContent("test-content");
		resources.delete();
		assertThat(this.root.resolve("test")).doesNotExist();
	}

	@Test
	void whenAddPackageAndDeleteThenResourcesDoNotExist() {
		Resources resources = new Resources(this.root);
		resources.addPackage(getClass().getPackage().getName(),
				new String[] { "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" });
		assertThat(this.root.resolve("resource-1.txt")).hasContent("one");
		assertThat(this.root.resolve("resource-2.txt")).hasContent("two");
		assertThat(this.root.resolve("sub/resource-3.txt")).hasContent("three");
		resources.delete();
		assertThat(this.root.resolve("resource-1.txt")).doesNotExist();
		assertThat(this.root.resolve("resource-2.txt")).doesNotExist();
		assertThat(this.root.resolve("sub/resource-3.txt")).doesNotExist();
		assertThat(this.root.resolve("sub")).doesNotExist();
	}

	@Test
	void whenAddDirectoryThenDirectoryIsCreated() {
		Resources resources = new Resources(this.root);
		resources.addDirectory("dir");
		assertThat(this.root.resolve("dir")).isDirectory();
	}

	@Test
	void whenAddDirectoryWithPathThenDirectoryIsCreated() {
		Resources resources = new Resources(this.root);
		resources.addDirectory("one/two/three/dir");
		assertThat(this.root.resolve("one/two/three/dir")).isDirectory();
	}

	@Test
	void whenAddDirectoryAndDirectoryAlreadyExistsThenDoesNotThrow() {
		Resources resources = new Resources(this.root);
		resources.addDirectory("one/two/three/dir");
		resources.addDirectory("one/two/three/dir");
		assertThat(this.root.resolve("one/two/three/dir")).isDirectory();
	}

	@Test
	void whenAddDirectoryAndResourceAlreadyExistsThenIllegalStateExceptionIsThrown() {
		Resources resources = new Resources(this.root);
		resources.addResource("one/two/three/", "content");
		assertThatIllegalStateException().isThrownBy(() -> resources.addDirectory("one/two/three"));
	}

	@Test
	void whenAddResourceAndDirectoryAlreadyExistsThenIllegalStateExceptionIsThrown() {
		Resources resources = new Resources(this.root);
		resources.addDirectory("one/two/three");
		assertThatIllegalStateException().isThrownBy(() -> resources.addResource("one/two/three", "content"));
	}

}
