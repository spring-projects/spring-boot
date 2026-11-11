/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restdocs.test.autoconfigure;

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
