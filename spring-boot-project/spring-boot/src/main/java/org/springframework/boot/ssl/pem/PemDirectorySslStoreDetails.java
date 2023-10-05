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

import org.springframework.util.Assert;

/**
 * Details to create a {@link PemDirectorySslStoreBundle}.
 *
 * @param directory the directory to load the certificate and key from
 * @param keyStoreType the type of keystore or {@code null}
 * @param trustStoreType the type of truststore or {@code null}
 * @param privateKeyPassword the password for loading the private key or {@code null}
 * @param alias the alias for the certificate and key or {@code null}
 * @param verifyKeys whether to verify the certificate and key
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public record PemDirectorySslStoreDetails(Path directory, String keyStoreType, String trustStoreType,
		String privateKeyPassword, String alias, boolean verifyKeys) {
	public PemDirectorySslStoreDetails {
		Assert.notNull(directory, "directory must not be null");
	}
}
