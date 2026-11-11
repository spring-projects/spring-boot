/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.PulsarClientException;

import static org.mockito.Mockito.mock;

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
		return "";
	}

	@Override
	public AuthenticationDataProvider getAuthData() {
		return mock(AuthenticationDataProvider.class);
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
