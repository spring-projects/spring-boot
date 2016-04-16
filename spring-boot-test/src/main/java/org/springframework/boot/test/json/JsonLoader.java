/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * Internal helper used to load JSON from various sources.
 *
 * @author Phillip Webb
 */
class JsonLoader {

	private final Class<?> resourceLoadClass;

	JsonLoader(Class<?> resourceLoadClass) {
		this.resourceLoadClass = resourceLoadClass;
	}

	public Class<?> getResourceLoadClass() {
		return this.resourceLoadClass;
	}

	public String getJson(CharSequence source) {
		if (source == null) {
			return null;
		}
		if (source.toString().endsWith(".json")) {
			return getJson(
					new ClassPathResource(source.toString(), this.resourceLoadClass));
		}
		return source.toString();
	}

	public String getJson(String path, Class<?> resourceLoadClass) {
		return getJson(new ClassPathResource(path, resourceLoadClass));
	}

	public String getJson(byte[] source) {
		return getJson(new ByteArrayInputStream(source));
	}

	public String getJson(File source) {
		try {
			return getJson(new FileInputStream(source));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from " + source, ex);
		}
	}

	public String getJson(Resource source) {
		try {
			return getJson(source.getInputStream());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from " + source, ex);
		}
	}

	public String getJson(InputStream source) {
		try {
			return FileCopyUtils.copyToString(new InputStreamReader(source));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load JSON from InputStream", ex);
		}
	}

}
