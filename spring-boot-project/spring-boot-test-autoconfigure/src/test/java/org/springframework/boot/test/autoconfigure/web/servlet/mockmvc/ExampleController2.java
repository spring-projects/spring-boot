/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Example {@link Controller} used with {@link WebMvcTest} tests.
 *
 * @author Phillip Webb
 */
@Controller
public class ExampleController2 {

	@GetMapping("/two")
	@ResponseBody
	public String one(ExampleArgument argument) {
		return argument + "two";
	}

	@GetMapping("/two/{id}")
	@ResponseBody
	public String one(@PathVariable ExampleId id) {
		return id.getId() + "two";
	}

}
