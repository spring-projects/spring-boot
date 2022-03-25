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

package org.springframework.boot.jackson.scan.a;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.jackson.JsonMixin;
import org.springframework.boot.jackson.Name;
import org.springframework.boot.jackson.NameAndAge;

@JsonMixin(type = { Name.class, NameAndAge.class })
public class RenameMixInClass {

	@JsonProperty("username")
	String getName() {
		return null;
	}

}
