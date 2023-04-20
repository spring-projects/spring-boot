/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class used to calculate digests.
 *
 * @author Phillip Webb
 */
final class Digest {

	private Digest() {
	}

	/**
	 * Return the SHA-1 digest from the supplied stream.
	 * @param supplier the stream supplier
	 * @return the SHA-1 digest
	 * @throws IOException on IO error
	 */
	static String sha1(InputStreamSupplier supplier) throws IOException {
		try {
			try (DigestInputStream inputStream = new DigestInputStream(supplier.openStream(),
					MessageDigest.getInstance("SHA-1"))) {
				inputStream.readAllBytes();
				return HexFormat.of().formatHex(inputStream.getMessageDigest().digest());
			}
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
