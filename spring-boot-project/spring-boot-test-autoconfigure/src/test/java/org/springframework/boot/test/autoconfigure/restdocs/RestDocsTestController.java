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

package org.springframework.boot.test.autoconfigure.restdocs;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestDocsTestController {

	@ResponseBody
	@RequestMapping(path = "/", produces = MediaTypes.HAL_JSON_VALUE)
	public Map<String, Object> index() {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> links = new HashMap<>();
		links.put("self", WebMvcLinkBuilder.linkTo(getClass()).toUri().toString());
		response.put("_links", links);
		return response;
	}

}
