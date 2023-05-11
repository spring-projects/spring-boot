/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class to map Pulsar auth parameters to well-known keys.
 *
 * @author Alexander Preuß
 */
enum WellKnownAuthParameters {

	TENANT_DOMAIN("tenantDomain"),

	TENANT_SERVICE("tenantService"),

	PROVIDER_DOMAIN("providerDomain"),

	PRIVATE_KEY("privateKey"),

	PRIVATE_KEY_PATH("privateKeyPath"),

	KEY_ID("keyId"),

	AUTO_PREFETCH_ENABLED("autoPrefetchEnabled"),

	ATHENZ_CONF_PATH("athenzConfPath"),

	PRINCIPAL_HEADER("principalHeader"),

	ROLE_HEADER("roleHeader"),

	ZTS_URL("ztsUrl"),

	USER_ID("userId"),

	PASSWORD("password"),

	KEY_STORE_TYPE("keyStoreType"),

	KEY_STORE_PATH("keyStorePath"),

	KEY_STORE_PASSWORD("keyStorePassword"),

	TYPE("type"),

	ISSUER_URL("issuerUrl"),

	AUDIENCE("audience"),

	SCOPE("scope"),

	SASL_JAAS_CLIENT_SECTION_NAME("saslJaasClientSectionName"),

	SERVER_TYPE("serverType"),

	TLS_CERT_FILE("tlsCertFile"),

	TLS_KEY_FILE("tlsKeyFile"),

	TOKEN("token");

	private static final Map<String, String> LOWER_CASE_TO_CAMEL_CASE = Arrays.stream(values())
		.map(WellKnownAuthParameters::getCamelCaseKey)
		.collect(Collectors.toMap(String::toLowerCase, Function.identity()));

	private final String camelCaseKey;

	WellKnownAuthParameters(String camelCaseKey) {
		this.camelCaseKey = camelCaseKey;
	}

	String getCamelCaseKey() {
		return this.camelCaseKey;
	}

	/**
	 * Returns the camel-cased version a Pulsar auth parameter or the given key in case it
	 * is not part of the well-known ones.
	 * @param lowerCaseKey the lower-cased auth parameter
	 * @return the camel-cased auth parameter, or the lowerCaseKey if the parameter is not
	 * found.
	 */
	public static String toCamelCaseKey(String lowerCaseKey) {
		return LOWER_CASE_TO_CAMEL_CASE.getOrDefault(lowerCaseKey, lowerCaseKey);
	}

}
