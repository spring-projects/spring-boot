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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.util.StringUtils;

/**
 * The JSON web token provided with each request that originates from Cloud Foundry.
 *
 * @author Madhura Bhave
 * @since 1.5.22
 */
public class Token {

	private final String encoded;

	private final String signature;

	private final Map<String, Object> header;

	private final Map<String, Object> claims;

	/**
     * Constructs a new Token object from the given encoded string.
     * 
     * @param encoded the encoded string representing the token
     * @throws CloudFoundryAuthorizationException if the encoded string is invalid or missing required components
     */
    public Token(String encoded) {
		this.encoded = encoded;
		int firstPeriod = encoded.indexOf('.');
		int lastPeriod = encoded.lastIndexOf('.');
		if (firstPeriod <= 0 || lastPeriod <= firstPeriod) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN,
					"JWT must have header, body and signature");
		}
		this.header = parseJson(encoded.substring(0, firstPeriod));
		this.claims = parseJson(encoded.substring(firstPeriod + 1, lastPeriod));
		this.signature = encoded.substring(lastPeriod + 1);
		if (!StringUtils.hasLength(this.signature)) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN,
					"Token must have non-empty crypto segment");
		}
	}

	/**
     * Parses a JSON string encoded in base64 format and returns a map of key-value pairs.
     * 
     * @param base64 the base64 encoded JSON string to be parsed
     * @return a map containing the parsed JSON data
     * @throws CloudFoundryAuthorizationException if the token could not be parsed
     */
    private Map<String, Object> parseJson(String base64) {
		try {
			byte[] bytes = Base64.getUrlDecoder().decode(base64);
			return JsonParserFactory.getJsonParser().parseMap(new String(bytes, StandardCharsets.UTF_8));
		}
		catch (RuntimeException ex) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN, "Token could not be parsed", ex);
		}
	}

	/**
     * Returns the content of the Token as a byte array.
     * 
     * @return the content of the Token as a byte array
     */
    public byte[] getContent() {
		return this.encoded.substring(0, this.encoded.lastIndexOf('.')).getBytes();
	}

	/**
     * Returns the signature as a byte array.
     *
     * @return the signature as a byte array
     */
    public byte[] getSignature() {
		return Base64.getUrlDecoder().decode(this.signature);
	}

	/**
     * Returns the signature algorithm used in the token.
     * 
     * @return the signature algorithm
     */
    public String getSignatureAlgorithm() {
		return getRequired(this.header, "alg", String.class);
	}

	/**
     * Returns the issuer of the token.
     * 
     * @return the issuer of the token
     */
    public String getIssuer() {
		return getRequired(this.claims, "iss", String.class);
	}

	/**
     * Returns the expiry time of the token.
     * 
     * @return the expiry time of the token
     */
    public long getExpiry() {
		return getRequired(this.claims, "exp", Integer.class).longValue();
	}

	/**
     * Retrieves the scope from the claims of the token.
     * 
     * @return The scope as a List of Strings.
     */
    @SuppressWarnings("unchecked")
	public List<String> getScope() {
		return getRequired(this.claims, "scope", List.class);
	}

	/**
     * Returns the key ID from the header of the token.
     * 
     * @return the key ID as a String
     */
    public String getKeyId() {
		return getRequired(this.header, "kid", String.class);
	}

	/**
     * Retrieves the value associated with the specified key from the given map.
     * 
     * @param map  the map from which to retrieve the value
     * @param key  the key associated with the value to be retrieved
     * @param type the expected type of the value
     * @return the value associated with the specified key
     * @throws CloudFoundryAuthorizationException if the value is not found or if the value type is unexpected
     */
    @SuppressWarnings("unchecked")
	private <T> T getRequired(Map<String, Object> map, String key, Class<T> type) {
		Object value = map.get(key);
		if (value == null) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN, "Unable to get value from key " + key);
		}
		if (!type.isInstance(value)) {
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN,
					"Unexpected value type from key " + key + " value " + value);
		}
		return (T) value;
	}

	/**
     * Returns the encoded string representation of the Token object.
     *
     * @return the encoded string representation of the Token object
     */
    @Override
	public String toString() {
		return this.encoded;
	}

}
