/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.restdocs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.assertj.core.api.Condition;
import org.assertj.core.description.TextDescription;

import org.springframework.util.FileCopyUtils;

/**
 * A {@link Condition} to assert that a file's contents contain a given string.
 *
 * @author Andy Wilkinson
 */
class ContentContainingCondition extends Condition<File> {

	private final String toContain;

	ContentContainingCondition(String toContain) {
		super(new TextDescription("content containing %s", toContain));
		this.toContain = toContain;
	}

	@Override
	public boolean matches(File value) {
		Reader reader = null;
		try {
			reader = new FileReader(value);
			String content = FileCopyUtils.copyToString(reader);
			return content.contains(this.toContain);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException ex) {
					// Ignore
				}
			}
		}
	}

}
