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

package org.springframework.boot.docs.io.validation;

import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * MyBean class.
 */
@Service
@Validated
public class MyBean {

	/**
     * Finds an archive by its code and author.
     * 
     * @param code   the code of the archive to find (must be between 8 and 10 characters long)
     * @param author the author of the archive
     * @return the found archive, or null if no archive is found
     */
    public Archive findByCodeAndAuthor(@Size(min = 8, max = 10) String code, Author author) {
		return /**/ null;
	}

}
