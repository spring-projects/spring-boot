/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sni.server;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SniServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SniServerApplication.class, args);
	}

}
