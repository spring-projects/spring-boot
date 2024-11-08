/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.ssl;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FixedTrustManagerFactory}.
 *
 * @author Moritz Halbritter
 */
class FixedTrustManagerFactoryTests {

	@Test
	void shouldReturnTrustmanagers() throws Exception {
		TrustManager trustManager1 = mock(TrustManager.class);
		TrustManager trustManager2 = mock(TrustManager.class);
		FixedTrustManagerFactory factory = FixedTrustManagerFactory.of(getDefaultTrustManagerFactory(), trustManager1,
				trustManager2);
		assertThat(factory.getTrustManagers()).containsExactly(trustManager1, trustManager2);
	}

	private static TrustManagerFactory getDefaultTrustManagerFactory() throws NoSuchAlgorithmException {
		return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	}

}
