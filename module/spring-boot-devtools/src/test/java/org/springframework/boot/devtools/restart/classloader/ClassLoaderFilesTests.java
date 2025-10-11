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

package org.springframework.boot.devtools.restart.classloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceDirectory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClassLoaderFiles}.
 *
 * @author Phillip Webb
 */
class ClassLoaderFilesTests {

	private final ClassLoaderFiles files = new ClassLoaderFiles();

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addFileNameMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.files.addFile(null, mock(ClassLoaderFile.class)))
			.withMessageContaining("'name' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addFileFileMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.files.addFile("test", null))
			.withMessageContaining("'file' must not be null");
	}

	@Test
	void getFileWithNullName() {
		assertThat(this.files.getFile(null)).isNull();
	}

	@Test
	void addAndGet() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("myfile", file);
		assertThat(this.files.getFile("myfile")).isEqualTo(file);
	}

	@Test
	void getMissing() {
		assertThat(this.files.getFile("missing")).isNull();
	}

	@Test
	void addTwice() {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		this.files.addFile("myfile", file1);
		this.files.addFile("myfile", file2);
		assertThat(this.files.getFile("myfile")).isEqualTo(file2);
	}

	@Test
	void addTwiceInDifferentSourceDirectories() {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		this.files.addFile("a", "myfile", file1);
		this.files.addFile("b", "myfile", file2);
		assertThat(this.files.getFile("myfile")).isEqualTo(file2);
		assertThat(this.files.getOrCreateSourceDirectory("a").getFiles()).isEmpty();
		assertThat(this.files.getOrCreateSourceDirectory("b").getFiles()).hasSize(1);
	}

	@Test
	void getSourceDirectories() {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		ClassLoaderFile file3 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		ClassLoaderFile file4 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		this.files.addFile("a", "myfile1", file1);
		this.files.addFile("a", "myfile2", file2);
		this.files.addFile("b", "myfile3", file3);
		this.files.addFile("b", "myfile4", file4);
		Iterator<SourceDirectory> sourceDirectories = this.files.getSourceDirectories().iterator();
		SourceDirectory sourceDirectory1 = sourceDirectories.next();
		SourceDirectory sourceDirectory2 = sourceDirectories.next();
		assertThat(sourceDirectories.hasNext()).isFalse();
		assertThat(sourceDirectory1.getName()).isEqualTo("a");
		assertThat(sourceDirectory2.getName()).isEqualTo("b");
		assertThat(sourceDirectory1.getFiles()).containsOnly(file1, file2);
		assertThat(sourceDirectory2.getFiles()).containsOnly(file3, file4);
	}

	@Test
	void serialize() throws Exception {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("myfile", file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(this.files);
		oos.close();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		ClassLoaderFiles readObject = (ClassLoaderFiles) ois.readObject();
		assertThat(readObject.getFile("myfile")).isNotNull();
	}

	@Test
	void addAll() {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("a", "myfile1", file1);
		ClassLoaderFiles toAdd = new ClassLoaderFiles();
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		ClassLoaderFile file3 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		toAdd.addFile("a", "myfile2", file2);
		toAdd.addFile("b", "myfile3", file3);
		this.files.addAll(toAdd);
		Iterator<SourceDirectory> sourceDirectories = this.files.getSourceDirectories().iterator();
		SourceDirectory sourceDirectory1 = sourceDirectories.next();
		SourceDirectory sourceDirectory2 = sourceDirectories.next();
		assertThat(sourceDirectories.hasNext()).isFalse();
		assertThat(sourceDirectory1.getName()).isEqualTo("a");
		assertThat(sourceDirectory2.getName()).isEqualTo("b");
		assertThat(sourceDirectory1.getFiles()).containsOnly(file1, file2);
	}

	@Test
	void getSize() {
		this.files.addFile("s1", "n1", mock(ClassLoaderFile.class));
		this.files.addFile("s1", "n2", mock(ClassLoaderFile.class));
		this.files.addFile("s2", "n3", mock(ClassLoaderFile.class));
		this.files.addFile("s2", "n1", mock(ClassLoaderFile.class));
		assertThat(this.files.size()).isEqualTo(3);
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void classLoaderFilesMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassLoaderFiles(null))
			.withMessageContaining("'classLoaderFiles' must not be null");
	}

	@Test
	void constructFromExistingSet() {
		this.files.addFile("s1", "n1", mock(ClassLoaderFile.class));
		this.files.addFile("s1", "n2", mock(ClassLoaderFile.class));
		ClassLoaderFiles copy = new ClassLoaderFiles(this.files);
		this.files.addFile("s2", "n3", mock(ClassLoaderFile.class));
		assertThat(this.files.size()).isEqualTo(3);
		assertThat(copy.size()).isEqualTo(2);
	}

}
