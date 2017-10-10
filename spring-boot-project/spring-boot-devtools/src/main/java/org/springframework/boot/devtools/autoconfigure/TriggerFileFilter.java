/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;
import java.io.FileFilter;

import org.springframework.util.Assert;

/**
 * {@link FileFilter} that accepts only a specific "trigger" file.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class TriggerFileFilter implements FileFilter {

	private final String name;

	public TriggerFileFilter(String name) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
	}

	@Override
	public boolean accept(File file) {
		return file.getName().equals(this.name);
	}

}
