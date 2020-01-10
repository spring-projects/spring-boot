/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Function;

import org.springframework.util.FileCopyUtils;

/**
 * Provides access to the contents of a file.
 *
 * @author Andy Wilkinson
 */
class FileContents {

	private final File file;

	FileContents(File file) {
		this.file = file;
	}

	String get() {
		return get(Function.identity());
	}

	<T> T get(Function<String, T> transformer) {
		if ((!this.file.exists()) || this.file.length() == 0) {
			return null;
		}
		try {
			return transformer.apply(FileCopyUtils.copyToString(new FileReader(this.file)));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public String toString() {
		return get();
	}

}
