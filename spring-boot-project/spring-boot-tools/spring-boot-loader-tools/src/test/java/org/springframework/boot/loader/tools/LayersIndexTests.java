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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LayersIndex}.
 *
 * @author Phillip Webb
 */
class LayersIndexTests {

	private static final Layer LAYER_A = new Layer("a");

	private static final Layer LAYER_B = new Layer("b");

	private static final Layer LAYER_C = new Layer("c");

	private String testMethodName;

	@BeforeEach
	void setup(TestInfo testInfo) {
		this.testMethodName = testInfo.getTestMethod().get().getName();
	}

	@Test
	void writeToWhenSimpleNamesSortsAlphabetically() throws Exception {
		LayersIndex index = new LayersIndex(LAYER_A);
		index.add(LAYER_A, "cat");
		index.add(LAYER_A, "dog");
		index.add(LAYER_A, "aardvark");
		index.add(LAYER_A, "zerbra");
		index.add(LAYER_A, "hamster");
		assertThatIndex(index).writesExpectedContent();
	}

	@Test
	void writeToWritesLayersInIteratorOrder() {
		LayersIndex index = new LayersIndex(LAYER_B, LAYER_A, LAYER_C);
		index.add(LAYER_A, "a1");
		index.add(LAYER_A, "a2");
		index.add(LAYER_B, "b1");
		index.add(LAYER_B, "b2");
		index.add(LAYER_C, "c1");
		index.add(LAYER_C, "c2");
		assertThatIndex(index).writesExpectedContent();
	}

	@Test
	void writeToWhenLayerNotUsedDoesNotSkipLayer() {
		LayersIndex index = new LayersIndex(LAYER_A, LAYER_B, LAYER_C);
		index.add(LAYER_A, "a1");
		index.add(LAYER_A, "a2");
		index.add(LAYER_C, "c1");
		index.add(LAYER_C, "c2");
		assertThatIndex(index).writesExpectedContent();
	}

	@Test
	void writeToWhenAllFilesInFolderAreInSameLayerUsesFolder() {
		LayersIndex index = new LayersIndex(LAYER_A, LAYER_B, LAYER_C);
		index.add(LAYER_A, "a1/b1/c1");
		index.add(LAYER_A, "a1/b1/c2");
		index.add(LAYER_A, "a1/b2/c1");
		index.add(LAYER_B, "a2/b1");
		index.add(LAYER_B, "a2/b2");
		assertThatIndex(index).writesExpectedContent();
	}

	@Test
	void writeToWhenAllFilesInFolderAreInNotInSameLayerUsesFiles() {
		LayersIndex index = new LayersIndex(LAYER_A, LAYER_B, LAYER_C);
		index.add(LAYER_A, "a1/b1/c1");
		index.add(LAYER_B, "a1/b1/c2");
		index.add(LAYER_C, "a1/b2/c1");
		index.add(LAYER_A, "a2/b1");
		index.add(LAYER_B, "a2/b2");
		assertThatIndex(index).writesExpectedContent();
	}

	@Test
	void writeToWhenSpaceInFileName() {
		LayersIndex index = new LayersIndex(LAYER_A);
		index.add(LAYER_A, "a b");
		index.add(LAYER_A, "a b/c");
		index.add(LAYER_A, "a b/d");
		assertThatIndex(index).writesExpectedContent();
	}

	private LayersIndexAssert assertThatIndex(LayersIndex index) {
		return new LayersIndexAssert(index);
	}

	private class LayersIndexAssert extends AbstractObjectAssert<LayersIndexAssert, LayersIndex> {

		LayersIndexAssert(LayersIndex actual) {
			super(actual, LayersIndexAssert.class);
		}

		void writesExpectedContent() {
			try {
				String actualContent = getContent();
				String name = "LayersIndexTests-" + LayersIndexTests.this.testMethodName + ".txt";
				InputStream in = LayersIndexTests.class.getResourceAsStream(name);
				Assert.state(in != null, "Can't read " + name);
				String expectedContent = new String(FileCopyUtils.copyToByteArray(in), StandardCharsets.UTF_8);
				expectedContent = expectedContent.replace("\r", "");
				assertThat(actualContent).isEqualTo(expectedContent);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}

		}

		private String getContent() throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			this.actual.writeTo(out);
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		}

	}

}
