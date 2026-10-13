/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Example {@link Controller @Controller} used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 */
@Controller
public class ExampleController2 {

	@GetMapping("/two")
	@ResponseBody
	public String two(ExampleArgument argument) {
		return argument + "two";
	}

	@GetMapping("/two/{id}")
	@ResponseBody
	public String two(@PathVariable ExampleId id) {
		return id.getId() + "two";
	}

	@PostMapping("/two/{id}")
	@ResponseBody
	public ExampleResult twoUpdate(@PathVariable String id) {
		return new ExampleResult(id);
	}

	@PostMapping("/two2/{id}")
	@ResponseBody
	@Deprecated(since = "4.0.0", forRemoval = true)
	@SuppressWarnings("removal")
	public ExampleResult2 two2Update(@PathVariable String id) {
		return new ExampleResult2(id);
	}

}
