/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath.resources;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
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

	private Resources resources;

	@BeforeEach
	void setUp() {
		this.resources = new Resources(this.root);
	}

	@Test
	void whenAddResourceThenResourceIsCreatedAndCanBeFound() {
		this.resources.addResource("test", "test-content", true);
		assertThat(this.root.resolve("test")).hasContent("test-content");
		assertThat(this.resources.find("test")).isNotNull();
	}

	@Test
	void whenAddResourceHasContentReferencingResourceRootThenResourceIsCreatedWithReferenceToRoot() {
		this.resources.addResource("test", "*** ${resourceRoot} ***", true);
		assertThat(this.root.resolve("test")).hasContent("*** " + this.root + " ***");
	}

	@Test
	void whenAddResourceWithPathThenResourceIsCreatedAndItAndItsAncestorsCanBeFound() {
		this.resources.addResource("a/b/c/test", "test-content", true);
		assertThat(this.root.resolve("a/b/c/test")).hasContent("test-content");
		assertThat(this.resources.find("a/b/c/test")).isNotNull();
		assertThat(this.resources.find("a/b/c/")).isNotNull();
		assertThat(this.resources.find("a/b/")).isNotNull();
		assertThat(this.resources.find("a/")).isNotNull();
	}

	@Test
	void whenAddResourceAndResourceAlreadyExistsThenResourcesIsOverwritten() {
		this.resources.addResource("a/b/c/test", "original-content", true);
		this.resources.addResource("a/b/c/test", "new-content", true);
		assertThat(this.root.resolve("a/b/c/test")).hasContent("new-content");
	}

	@Test
	void whenAddPackageThenNamedResourcesFromPackageAreCreatedAndCanBeFound() {
		this.resources.addPackage(getClass().getPackage().getName(),
				new String[] { "resource-1.txt", "sub/resource-3.txt" });
		assertThat(this.root.resolve("resource-1.txt")).hasContent("one");
		assertThat(this.root.resolve("resource-2.txt")).doesNotExist();
		assertThat(this.root.resolve("sub/resource-3.txt")).hasContent("three");
		assertThat(this.resources.find("resource-1.txt")).isNotNull();
		assertThat(this.resources.find("resource-2.txt")).isNull();
		assertThat(this.resources.find("sub/resource-3.txt")).isNotNull();
		assertThat(this.resources.find("sub/")).isNotNull();
	}

	@Test
	void whenAddResourceAndDeleteThenResourceDoesNotExistAndCannotBeFound() {
		this.resources.addResource("test", "test-content", true);
		assertThat(this.root.resolve("test")).hasContent("test-content");
		assertThat(this.resources.find("test")).isNotNull();
		this.resources.delete();
		assertThat(this.root.resolve("test")).doesNotExist();
		assertThat(this.resources.find("test")).isNull();
	}

	@Test
	void whenAddPackageAndDeleteThenResourcesDoNotExistAndCannotBeFound() {
		this.resources.addPackage(getClass().getPackage().getName(),
				new String[] { "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" });
		assertThat(this.root.resolve("resource-1.txt")).hasContent("one");
		assertThat(this.root.resolve("resource-2.txt")).hasContent("two");
		assertThat(this.root.resolve("sub/resource-3.txt")).hasContent("three");
		assertThat(this.resources.find("resource-1.txt")).isNotNull();
		assertThat(this.resources.find("resource-2.txt")).isNotNull();
		assertThat(this.resources.find("sub/resource-3.txt")).isNotNull();
		assertThat(this.resources.find("sub/")).isNotNull();
		this.resources.delete();
		assertThat(this.root.resolve("resource-1.txt")).doesNotExist();
		assertThat(this.root.resolve("resource-2.txt")).doesNotExist();
		assertThat(this.root.resolve("sub/resource-3.txt")).doesNotExist();
		assertThat(this.root.resolve("sub")).doesNotExist();
		assertThat(this.resources.find("resource-1.txt")).isNull();
		assertThat(this.resources.find("resource-2.txt")).isNull();
		assertThat(this.resources.find("sub/resource-3.txt")).isNull();
		assertThat(this.resources.find("sub/")).isNull();
	}

	@Test
	void whenAddDirectoryThenDirectoryIsCreatedAndCanBeFound() {
		this.resources.addDirectory("dir");
		assertThat(this.root.resolve("dir")).isDirectory();
		assertThat(this.resources.find("dir/")).isNotNull();
	}

	@Test
	void whenAddDirectoryWithPathThenDirectoryIsCreatedAndItAndItsAncestorsCanBeFound() {
		this.resources.addDirectory("one/two/three/dir");
		assertThat(this.root.resolve("one/two/three/dir")).isDirectory();
		assertThat(this.resources.find("one/two/three/dir/")).isNotNull();
		assertThat(this.resources.find("one/two/three/")).isNotNull();
		assertThat(this.resources.find("one/two/")).isNotNull();
		assertThat(this.resources.find("one/")).isNotNull();
	}

	@Test
	void whenAddDirectoryAndDirectoryAlreadyExistsThenDoesNotThrow() {
		this.resources.addDirectory("one/two/three/dir");
		this.resources.addDirectory("one/two/three/dir");
		assertThat(this.root.resolve("one/two/three/dir")).isDirectory();
	}

	@Test
	void whenAddDirectoryAndResourceAlreadyExistsThenIllegalStateExceptionIsThrown() {
		this.resources.addResource("one/two/three/", "content", true);
		assertThatIllegalStateException().isThrownBy(() -> this.resources.addDirectory("one/two/three"));
	}

	@Test
	void whenAddResourceAndDirectoryAlreadyExistsThenIllegalStateExceptionIsThrown() {
		this.resources.addDirectory("one/two/three");
		assertThatIllegalStateException()
			.isThrownBy(() -> this.resources.addResource("one/two/three", "content", true));
	}

}
