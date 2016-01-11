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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessDataFile;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CentralDirectoryParser}.
 *
 * @author Phillip Webb
 */
public class CentralDirectoryParserTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File jarFile;

	private RandomAccessData jarData;

	@Before
	public void setup() throws Exception {
		this.jarFile = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(this.jarFile);
		this.jarData = new RandomAccessDataFile(this.jarFile);
	}

	@Test
	public void vistsInOrder() throws Exception {
		CentralDirectoryVistor vistor = mock(CentralDirectoryVistor.class);
		CentralDirectoryParser parser = new CentralDirectoryParser();
		parser.addVistor(vistor);
		parser.parse(this.jarData, false);
		InOrder ordered = inOrder(vistor);
		ordered.verify(vistor).visitStart(any(CentralDirectoryEndRecord.class),
				any(RandomAccessData.class));
		ordered.verify(vistor, atLeastOnce())
				.visitFileHeader(any(CentralDirectoryFileHeader.class), anyInt());
		ordered.verify(vistor).visitEnd();
	}

	@Test
	public void vistRecords() throws Exception {
		Collector collector = new Collector();
		CentralDirectoryParser parser = new CentralDirectoryParser();
		parser.addVistor(collector);
		parser.parse(this.jarData, false);
		Iterator<CentralDirectoryFileHeader> headers = collector.getHeaders().iterator();
		assertThat(headers.next().getName().toString(), equalTo("META-INF/"));
		assertThat(headers.next().getName().toString(), equalTo("META-INF/MANIFEST.MF"));
		assertThat(headers.next().getName().toString(), equalTo("1.dat"));
		assertThat(headers.next().getName().toString(), equalTo("2.dat"));
		assertThat(headers.next().getName().toString(), equalTo("d/"));
		assertThat(headers.next().getName().toString(), equalTo("d/9.dat"));
		assertThat(headers.next().getName().toString(), equalTo("special/"));
		assertThat(headers.next().getName().toString(), equalTo("special/\u00EB.dat"));
		assertThat(headers.next().getName().toString(), equalTo("nested.jar"));
		assertThat(headers.next().getName().toString(), equalTo("another-nested.jar"));
		assertThat(headers.hasNext(), equalTo(false));
	}

	private static class Collector implements CentralDirectoryVistor {

		private List<CentralDirectoryFileHeader> headers = new ArrayList<CentralDirectoryFileHeader>();

		@Override
		public void visitStart(CentralDirectoryEndRecord endRecord,
				RandomAccessData centralDirectoryData) {
		}

		@Override
		public void visitFileHeader(CentralDirectoryFileHeader fileHeader,
				int dataOffset) {
			this.headers.add(fileHeader);
		}

		@Override
		public void visitEnd() {
		}

		public List<CentralDirectoryFileHeader> getHeaders() {
			return this.headers;
		}

	}

}
