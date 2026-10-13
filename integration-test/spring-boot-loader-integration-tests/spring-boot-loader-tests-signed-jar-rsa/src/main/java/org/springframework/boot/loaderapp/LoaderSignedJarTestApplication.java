/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loaderapp;

import java.security.Security;
import javax.crypto.Cipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoaderSignedJarTestApplication {

	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		Cipher.getInstance("AES/CBC/PKCS5Padding","BC");
		System.out.println("Legion of the Bouncy Castle");
		SpringApplication.run(LoaderSignedJarTestApplication.class, args);
	}

}
