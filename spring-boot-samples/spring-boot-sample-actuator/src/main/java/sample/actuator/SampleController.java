/*
 * Copyright 2012-2013 the original author or authors.
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

package sample.actuator;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Description("A controller for handling requests for hello messages")
public class SampleController {

	@Autowired
	private HelloWorldService helloWorldService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, String> hello() {
		return Collections.singletonMap("message",
				this.helloWorldService.getHelloMessage());
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> olleh(@Validated Message message) {
		Map<String, Object> model = new LinkedHashMap<String, Object>();
		model.put("message", message.getValue());
		model.put("title", "Hello Home");
		model.put("date", new Date());
		return model;
	}

	@RequestMapping("/foo")
	@ResponseBody
	public String foo() {
		throw new IllegalArgumentException("Server error");
	}

	protected static class Message {

		@NotBlank(message = "Message value cannot be empty")
		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

}
