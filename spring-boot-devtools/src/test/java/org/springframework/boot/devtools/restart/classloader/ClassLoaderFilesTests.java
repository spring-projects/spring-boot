/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.restart.classloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceFolder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClassLoaderFiles}.
 *
 * @author Phillip Webb
 */
public class ClassLoaderFilesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ClassLoaderFiles files = new ClassLoaderFiles();

	@Test
	public void addFileNameMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		this.files.addFile(null, mock(ClassLoaderFile.class));
	}

	@Test
	public void addFileFileMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("File must not be null");
		this.files.addFile("test", null);
	}

	@Test
	public void getFileWithNullName() throws Exception {
		assertThat(this.files.getFile(null), nullValue());
	}

	@Test
	public void addAndGet() throws Exception {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("myfile", file);
		assertThat(this.files.getFile("myfile"), equalTo(file));
	}

	@Test
	public void getMissing() throws Exception {
		assertThat(this.files.getFile("missing"), nullValue());
	}

	@Test
	public void addTwice() throws Exception {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		this.files.addFile("myfile", file1);
		this.files.addFile("myfile", file2);
		assertThat(this.files.getFile("myfile"), equalTo(file2));
	}

	@Test
	public void addTwiceInDifferentSourceFolders() throws Exception {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		this.files.addFile("a", "myfile", file1);
		this.files.addFile("b", "myfile", file2);
		assertThat(this.files.getFile("myfile"), equalTo(file2));
		assertThat(this.files.getOrCreateSourceFolder("a").getFiles().size(), equalTo(0));
		assertThat(this.files.getOrCreateSourceFolder("b").getFiles().size(), equalTo(1));
	}

	@Test
	public void getSourceFolders() throws Exception {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		ClassLoaderFile file3 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		ClassLoaderFile file4 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		this.files.addFile("a", "myfile1", file1);
		this.files.addFile("a", "myfile2", file2);
		this.files.addFile("b", "myfile3", file3);
		this.files.addFile("b", "myfile4", file4);
		Iterator<SourceFolder> sourceFolders = this.files.getSourceFolders().iterator();
		SourceFolder sourceFolder1 = sourceFolders.next();
		SourceFolder sourceFolder2 = sourceFolders.next();
		assertThat(sourceFolders.hasNext(), equalTo(false));
		assertThat(sourceFolder1.getName(), equalTo("a"));
		assertThat(sourceFolder2.getName(), equalTo("b"));
		assertThat(new ArrayList<ClassLoaderFile>(sourceFolder1.getFiles()),
				equalTo(Arrays.asList(file1, file2)));
		assertThat(new ArrayList<ClassLoaderFile>(sourceFolder2.getFiles()),
				equalTo(Arrays.asList(file3, file4)));
	}

	@Test
	public void serialzie() throws Exception {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("myfile", file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(this.files);
		oos.close();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
				bos.toByteArray()));
		ClassLoaderFiles readObject = (ClassLoaderFiles) ois.readObject();
		assertThat(readObject.getFile("myfile"), notNullValue());
	}

	@Test
	public void addAll() throws Exception {
		ClassLoaderFile file1 = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("a", "myfile1", file1);
		ClassLoaderFiles toAdd = new ClassLoaderFiles();
		ClassLoaderFile file2 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		ClassLoaderFile file3 = new ClassLoaderFile(Kind.MODIFIED, new byte[10]);
		toAdd.addFile("a", "myfile2", file2);
		toAdd.addFile("b", "myfile3", file3);
		this.files.addAll(toAdd);
		Iterator<SourceFolder> sourceFolders = this.files.getSourceFolders().iterator();
		SourceFolder sourceFolder1 = sourceFolders.next();
		SourceFolder sourceFolder2 = sourceFolders.next();
		assertThat(sourceFolders.hasNext(), equalTo(false));
		assertThat(sourceFolder1.getName(), equalTo("a"));
		assertThat(sourceFolder2.getName(), equalTo("b"));
		assertThat(new ArrayList<ClassLoaderFile>(sourceFolder1.getFiles()),
				equalTo(Arrays.asList(file1, file2)));
	}

	@Test
	public void getSize() throws Exception {
		this.files.addFile("s1", "n1", mock(ClassLoaderFile.class));
		this.files.addFile("s1", "n2", mock(ClassLoaderFile.class));
		this.files.addFile("s2", "n3", mock(ClassLoaderFile.class));
		this.files.addFile("s2", "n1", mock(ClassLoaderFile.class));
		assertThat(this.files.size(), equalTo(3));
	}

	@Test
	public void classLoaderFilesMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassLoaderFiles must not be null");
		new ClassLoaderFiles(null);
	}

	@Test
	public void constructFromExistingSet() throws Exception {
		this.files.addFile("s1", "n1", mock(ClassLoaderFile.class));
		this.files.addFile("s1", "n2", mock(ClassLoaderFile.class));
		ClassLoaderFiles copy = new ClassLoaderFiles(this.files);
		this.files.addFile("s2", "n3", mock(ClassLoaderFile.class));
		assertThat(this.files.size(), equalTo(3));
		assertThat(copy.size(), equalTo(2));
	}

}
