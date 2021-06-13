/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.io;

import java.io.IOException;

/**
 * Interface that can be used to write a file/directory layout.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public interface Layout {

	/**
	 * Add a directory to the content.
	 * @param name the full name of the directory to add
	 * @param owner the owner of the directory
	 * @throws IOException on IO error
	 */
	default void directory(String name, Owner owner) throws IOException {
		directory(name, owner, 0755);
	}

	/**
	 * Add a directory to the content.
	 * @param name the full name of the directory to add
	 * @param owner the owner of the directory
	 * @param mode the permissions for the file
	 * @throws IOException on IO error
	 */
	void directory(String name, Owner owner, int mode) throws IOException;

	/**
	 * Write a file to the content.
	 * @param name the full name of the file to add
	 * @param owner the owner of the file
	 * @param content the content to add
	 * @throws IOException on IO error
	 */
	default void file(String name, Owner owner, Content content) throws IOException {
		file(name, owner, 0644, content);
	}

	/**
	 * Write a file to the content.
	 * @param name the full name of the file to add
	 * @param owner the owner of the file
	 * @param mode the permissions for the file
	 * @param content the content to add
	 * @throws IOException on IO error
	 */
	void file(String name, Owner owner, int mode, Content content) throws IOException;

}
