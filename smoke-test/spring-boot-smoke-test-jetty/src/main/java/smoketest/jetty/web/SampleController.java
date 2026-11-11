/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty.web;

import jakarta.servlet.http.HttpServletResponse;
import smoketest.jetty.service.HelloWorldService;
import smoketest.jetty.service.HttpHeaderService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController {

	private final HelloWorldService helloWorldService;

	private final HttpHeaderService httpHeaderService;

	public SampleController(HelloWorldService helloWorldService, HttpHeaderService httpHeaderService) {
		this.helloWorldService = helloWorldService;
		this.httpHeaderService = httpHeaderService;
	}

	@GetMapping("/")
	@ResponseBody
	public String helloWorld() {
		return this.helloWorldService.getHelloMessage();
	}

	@GetMapping("/max-http-response-header")
	@ResponseBody
	public String maxHttpResponseHeader(HttpServletResponse response) {
		String headerValue = this.httpHeaderService.getHeaderValue();
		response.addHeader("x-max-header", headerValue);
		return this.helloWorldService.getHelloMessage();
	}

}
