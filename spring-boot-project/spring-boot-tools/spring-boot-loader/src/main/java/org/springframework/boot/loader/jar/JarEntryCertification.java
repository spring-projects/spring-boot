/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.security.CodeSigner;
import java.security.cert.Certificate;

/**
 * {@link Certificate} and {@link CodeSigner} details for a {@link JarEntry} from a signed
 * {@link JarFile}.
 *
 * @author Phillip Webb
 */
class JarEntryCertification {

	static final JarEntryCertification NONE = new JarEntryCertification(null, null);

	private final Certificate[] certificates;

	private final CodeSigner[] codeSigners;

	JarEntryCertification(Certificate[] certificates, CodeSigner[] codeSigners) {
		this.certificates = certificates;
		this.codeSigners = codeSigners;
	}

	Certificate[] getCertificates() {
		return (this.certificates != null) ? this.certificates.clone() : null;
	}

	CodeSigner[] getCodeSigners() {
		return (this.codeSigners != null) ? this.codeSigners.clone() : null;
	}

	static JarEntryCertification from(java.util.jar.JarEntry certifiedEntry) {
		Certificate[] certificates = (certifiedEntry != null) ? certifiedEntry.getCertificates() : null;
		CodeSigner[] codeSigners = (certifiedEntry != null) ? certifiedEntry.getCodeSigners() : null;
		if (certificates == null && codeSigners == null) {
			return NONE;
		}
		return new JarEntryCertification(certificates, codeSigners);
	}

}
