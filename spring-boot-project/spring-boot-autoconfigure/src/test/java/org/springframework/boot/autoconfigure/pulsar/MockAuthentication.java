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

package org.springframework.boot.autoconfigure.pulsar;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Test plugin-class-name for Authentication
 *
 * @author Swamy Mavuri
 */
@SuppressWarnings("deprecation")
public class MockAuthentication implements Authentication {

	public Map<String, String> authParamsMap = new HashMap<>();

	@Override
	public String getAuthMethodName() {
		return null;
	}

	@Override
	public AuthenticationDataProvider getAuthData() {
		return null;
	}

	@Override
	public void configure(Map<String, String> authParams) {
		this.authParamsMap = authParams;
	}

	@Override
	public void start() throws PulsarClientException {

	}

	@Override
	public void close() throws IOException {

	}

}
