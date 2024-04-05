/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.convert;

import java.io.File;
import java.io.IOException;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;

/**
 * {@link Converter} to convert from a {@link String} to a {@link File}. Supports basic
 * file conversion as well as file URLs.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class StringToFileConverter implements Converter<String, File> {

	@Override
	public File convert(String source) {
		Resource resource = new ApplicationResourceLoader().getResource(source);
		return getFile(resource);
	}

	private File getFile(Resource resource) {
		try {
			return resource.getFile();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not retrieve file for " + resource + ": " + ex.getMessage());
		}
	}

}
