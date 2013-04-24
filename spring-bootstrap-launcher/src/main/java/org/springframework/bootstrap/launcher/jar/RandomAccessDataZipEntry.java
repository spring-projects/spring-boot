/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.bootstrap.launcher.jar;

import java.util.zip.ZipEntry;

import org.springframework.bootstrap.launcher.data.RandomAccessData;

/**
 * A {@link ZipEntry} returned from a {@link RandomAccessDataZipInputStream}.
 * 
 * @author Phillip Webb
 */
public class RandomAccessDataZipEntry extends ZipEntry {

	private RandomAccessData data;

	/**
	 * Create new {@link RandomAccessDataZipEntry} instance.
	 * @param entry the underying {@link ZipEntry}
	 * @param data the entry data
	 */
	public RandomAccessDataZipEntry(ZipEntry entry, RandomAccessData data) {
		super(entry);
		this.data = data;
	}

	/**
	 * Returns the {@link RandomAccessData} for this entry.
	 * @return the entry data
	 */
	public RandomAccessData getData() {
		return data;
	}
}
