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

package org.springframework.boot.context.embedded.tomcat;

import java.security.KeyStore;

/**
 * Simple abstraction for providing {@link KeyStore keyStore} and {@link KeyStore
 * trustStore} instances.
 * @author Venil Noronha
 */
public interface StoreAware {

	/**
	 * Sets the {@link KeyStore keyStore}.
	 * @param keyStore the {@link KeyStore keyStore} to set.
	 */
	void setKeyStore(KeyStore keyStore);

	/**
	 * Sets the {@link KeyStore trustStore}.
	 * @param trustStore the {@link KeyStore trustStore} to set.
	 */
	void setTrustStore(KeyStore trustStore);

}
