/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader.data;

import org.junit.Test;

import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ByteArrayRandomAccessData}.
 *
 * @author Phillip Webb
 */
public class ByteArrayRandomAccessDataTests {

	@Test
	public void testGetInputStream() throws Exception {
		byte[] bytes = new byte[] { 0, 1, 2, 3, 4, 5 };
		RandomAccessData data = new ByteArrayRandomAccessData(bytes);
		assertThat(FileCopyUtils.copyToByteArray(
				data.getInputStream(ResourceAccess.PER_READ)), equalTo(bytes));
		assertThat(data.getSize(), equalTo((long) bytes.length));
	}

	@Test
	public void testGetSubsection() throws Exception {
		byte[] bytes = new byte[] { 0, 1, 2, 3, 4, 5 };
		RandomAccessData data = new ByteArrayRandomAccessData(bytes);
		data = data.getSubsection(1, 4).getSubsection(1, 2);
		assertThat(
				FileCopyUtils
						.copyToByteArray(data.getInputStream(ResourceAccess.PER_READ)),
				equalTo(new byte[] { 2, 3 }));
		assertThat(data.getSize(), equalTo(2L));
	}
}
