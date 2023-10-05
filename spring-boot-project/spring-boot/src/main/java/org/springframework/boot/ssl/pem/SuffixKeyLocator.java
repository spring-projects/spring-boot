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

import java.nio.file.Path;
import java.util.Collection;

import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.Certificate;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.KeyLocator;
import org.springframework.util.Assert;

/**
 * A {@link KeyLocator} which matches files with a given suffix.
 *
 * @author Moritz Halbritter
 */
class SuffixKeyLocator implements KeyLocator {

	private final String certificateSuffix;

	private final String keySuffix;

	SuffixKeyLocator(String certificateSuffix, String keySuffix) {
		Assert.notNull(certificateSuffix, "certificateSuffix must not be null");
		Assert.notNull(keySuffix, "keySuffix must not be null");
		this.certificateSuffix = certificateSuffix;
		this.keySuffix = keySuffix;
	}

	@Override
	public Path locate(Certificate certificate, Collection<Path> files) {
		String path = certificate.file().toString();
		if (!path.endsWith(this.certificateSuffix)) {
			throw new IllegalArgumentException(
					"Path '%s' does not end with '%s'".formatted(path, this.certificateSuffix));
		}
		path = path.substring(0, path.length() - this.certificateSuffix.length());
		path = path + this.keySuffix;
		return Path.of(path);
	}

}
