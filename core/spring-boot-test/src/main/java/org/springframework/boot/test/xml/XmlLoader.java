/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Contract;
import org.springframework.util.FileCopyUtils;

/**
 * Internal helper used to load XML from various sources.
 *
 * @author Tiziano Basile
 */
class XmlLoader {

	private final Class<?> resourceLoadClass;

	private final Charset charset;

	XmlLoader(Class<?> resourceLoadClass, @Nullable Charset charset) {
		this.resourceLoadClass = resourceLoadClass;
		this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
	}

	Class<?> getResourceLoadClass() {
		return this.resourceLoadClass;
	}

	@Contract("!null -> !null")
	@Nullable String getXml(@Nullable CharSequence source) {
		if (source == null) {
			return null;
		}
		if (source.toString().endsWith(".xml")) {
			return getXml(new ClassPathResource(source.toString(), this.resourceLoadClass));
		}
		return source.toString();
	}

	String getXml(String path, Class<?> resourceLoadClass) {
		return getXml(new ClassPathResource(path, resourceLoadClass));
	}

	String getXml(byte[] source) {
		return getXml(new ByteArrayInputStream(source));
	}

	String getXml(File source) {
		try {
			return getXml(new FileInputStream(source));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load XML from " + source, ex);
		}
	}

	String getXml(Resource source) {
		try {
			return getXml(source.getInputStream());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load XML from " + source, ex);
		}
	}

	String getXml(InputStream source) {
		try {
			return FileCopyUtils.copyToString(new InputStreamReader(source, this.charset));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load XML from InputStream", ex);
		}
	}

}
