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

package org.springframework.boot.loaderapp;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;

import jakarta.servlet.ServletContext;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.FileCopyUtils;

/**
 * LoaderTestApplication class.
 */
@SpringBootApplication
public class LoaderTestApplication {

	/**
     * This method returns a CommandLineRunner object that can be used to execute code when the application starts.
     * It takes a ServletContext object as a parameter.
     * 
     * @param servletContext The ServletContext object used to retrieve the resource URL.
     * @return A CommandLineRunner object that executes the code when the application starts.
     */
    @Bean
	public CommandLineRunner commandLineRunner(ServletContext servletContext) {
		return (args) -> {
			File temp = new File(System.getProperty("java.io.tmpdir"));
			URL resourceUrl = servletContext.getResource("webjars/jquery/3.5.0/jquery.js");
			JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
			String jarName = connection.getJarFile().getName();
			System.out.println(">>>>> jar file " + jarName);
			if(jarName.contains(temp.getAbsolutePath())) {
				System.out.println(">>>>> jar written to temp");
			}
			byte[] resourceContent = FileCopyUtils.copyToByteArray(resourceUrl.openStream());
			URL directUrl = new URL(resourceUrl.toExternalForm());
			byte[] directContent = FileCopyUtils.copyToByteArray(directUrl.openStream());
			String message = (!Arrays.equals(resourceContent, directContent)) ? "NO MATCH"
					: directContent.length + " BYTES";
			System.out.println(">>>>> " + message + " from " + resourceUrl);
		};
	}

	/**
     * The main method is the entry point of the application.
     * It starts the Spring Boot application by calling the SpringApplication.run() method
     * with the LoaderTestApplication class and the command line arguments as parameters.
     * After the application is started, the close() method is called to gracefully shut down the application.
     *
     * @param args The command line arguments passed to the application.
     */
    public static void main(String[] args) {
		SpringApplication.run(LoaderTestApplication.class, args).close();
	}

}
