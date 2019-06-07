/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link RestController} used by {@link WebMvcTestHateoasIntegrationTests}.
 *
 * @author Andy Wilkinson
 */
@RestController
@RequestMapping("/hateoas")
class HateoasController {

	@RequestMapping("/resource")
	public Resource<Map<String, String>> resource() {
		return new Resource<>(new HashMap<String, String>(), new Link("self", "https://api.example.com"));
	}

	@RequestMapping("/plain")
	public Map<String, String> plain() {
		return new HashMap<>();
	}

}
