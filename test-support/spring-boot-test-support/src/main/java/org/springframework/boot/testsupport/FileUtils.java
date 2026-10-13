/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utilities when working with {@link File files}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class FileUtils {

	private static final int BUFFER_SIZE = 32 * 1024;

	/**
	 * Generate a SHA-1 Hash for a given file.
	 * @param file the file to hash
	 * @return the hash value as a String
	 * @throws IOException if the file cannot be read
	 */
	public static String sha1Hash(File file) throws IOException {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			try (InputStream inputStream = new FileInputStream(file)) {
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					messageDigest.update(buffer, 0, bytesRead);
				}
				return HexFormat.of().formatHex(messageDigest.digest());
			}
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
