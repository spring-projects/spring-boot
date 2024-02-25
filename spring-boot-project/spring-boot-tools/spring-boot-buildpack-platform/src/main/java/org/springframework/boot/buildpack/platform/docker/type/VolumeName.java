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

package org.springframework.boot.buildpack.platform.docker.type;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * A Docker volume name.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class VolumeName {

	private final String value;

	/**
     * Constructs a new VolumeName object with the specified value.
     *
     * @param value the value of the volume name
     */
    private VolumeName(String value) {
		this.value = value;
	}

	/**
     * Compares this VolumeName object to the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the specified object is equal to this VolumeName object, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((VolumeName) obj).value);
	}

	/**
     * Returns the hash code value for this VolumeName object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.value.hashCode();
	}

	/**
     * Returns the string representation of the VolumeName object.
     *
     * @return the string representation of the VolumeName object
     */
    @Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a new {@link VolumeName} with a random name.
	 * @param prefix the prefix to use with the random name
	 * @return a randomly named volume
	 */
	public static VolumeName random(String prefix) {
		return random(prefix, 10);
	}

	/**
	 * Factory method to create a new {@link VolumeName} with a random name.
	 * @param prefix the prefix to use with the random name
	 * @param randomLength the number of chars in the random part of the name
	 * @return a randomly named volume reference
	 */
	public static VolumeName random(String prefix, int randomLength) {
		return of(RandomString.generate(prefix, randomLength));
	}

	/**
	 * Factory method to create a new {@link VolumeName} based on an object. The resulting
	 * name will be based off a SHA-256 digest of the given object's {@code toString()}
	 * method.
	 * @param <S> the source object type
	 * @param source the source object
	 * @param prefix the prefix to use with the volume name
	 * @param suffix the suffix to use with the volume name
	 * @param digestLength the number of chars in the digest part of the name
	 * @return a name based off the image reference
	 */
	public static <S> VolumeName basedOn(S source, String prefix, String suffix, int digestLength) {
		return basedOn(source, Object::toString, prefix, suffix, digestLength);
	}

	/**
	 * Factory method to create a new {@link VolumeName} based on an object. The resulting
	 * name will be based off a SHA-256 digest of the given object's name.
	 * @param <S> the source object type
	 * @param source the source object
	 * @param nameExtractor a method to extract the name of the object
	 * @param prefix the prefix to use with the volume name
	 * @param suffix the suffix to use with the volume name
	 * @param digestLength the number of chars in the digest part of the name
	 * @return a name based off the image reference
	 */
	public static <S> VolumeName basedOn(S source, Function<S, String> nameExtractor, String prefix, String suffix,
			int digestLength) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(nameExtractor, "NameExtractor must not be null");
		Assert.notNull(prefix, "Prefix must not be null");
		Assert.notNull(suffix, "Suffix must not be null");
		return of(prefix + getDigest(nameExtractor.apply(source), digestLength) + suffix);
	}

	/**
     * Returns the SHA-256 digest of the given name as a hexadecimal string.
     * 
     * @param name   the name to calculate the digest for
     * @param length the desired length of the hexadecimal string
     * @return the SHA-256 digest of the name as a hexadecimal string
     * @throws IllegalStateException if the SHA-256 algorithm is not available
     */
    private static String getDigest(String name, int length) {
		try {
			MessageDigest digest = MessageDigest.getInstance("sha-256");
			return asHexString(digest.digest(name.getBytes(StandardCharsets.UTF_8)), length);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Converts a byte array to a hexadecimal string representation.
     * 
     * @param digest the byte array to convert
     * @param digestLength the length of the digest to convert
     * @return the hexadecimal string representation of the digest
     * @throws IllegalArgumentException if the digestLength is greater than the length of the digest
     */
    private static String asHexString(byte[] digest, int digestLength) {
		Assert.isTrue(digestLength <= digest.length,
				() -> "DigestLength must be less than or equal to " + digest.length);
		return HexFormat.of().formatHex(digest, 0, digestLength);
	}

	/**
	 * Factory method to create a {@link VolumeName} with a specific value.
	 * @param value the volume reference value
	 * @return a new {@link VolumeName} instance
	 */
	public static VolumeName of(String value) {
		Assert.notNull(value, "Value must not be null");
		return new VolumeName(value);
	}

}
