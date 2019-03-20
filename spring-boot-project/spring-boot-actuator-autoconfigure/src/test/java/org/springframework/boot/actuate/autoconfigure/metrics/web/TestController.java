/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller used by metrics tests.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
@RestController
public class TestController {

	@GetMapping("test0")
	public String test0() {
		return "test0";
	}

	@GetMapping("test1")
	public String test1() {
		return "test1";
	}

	@GetMapping("test2")
	public String test2() {
		return "test2";
	}

}
