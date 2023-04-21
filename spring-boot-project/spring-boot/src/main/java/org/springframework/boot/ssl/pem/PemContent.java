/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

/**
 * Utility to load PEM content.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class PemContent {

	private static final Pattern PEM_HEADER = Pattern.compile("-+BEGIN\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

	private static final Pattern PEM_FOOTER = Pattern.compile("-+END\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

	private PemContent() {
	}

	static String load(String content) {
		if (content == null || isPemContent(content)) {
			return content;
		}
		try {
			URL url = ResourceUtils.getURL(content);
			try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
				return FileCopyUtils.copyToString(reader);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Error reading certificate or key from file '" + content + "':" + ex.getMessage(), ex);
		}
	}

	private static boolean isPemContent(String content) {
		return content != null && PEM_HEADER.matcher(content).find() && PEM_FOOTER.matcher(content).find();
	}

}
