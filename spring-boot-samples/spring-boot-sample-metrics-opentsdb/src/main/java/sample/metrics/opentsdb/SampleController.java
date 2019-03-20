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

package sample.metrics.opentsdb;

import java.util.Collections;
import java.util.Map;

import org.hibernate.validator.constraints.NotBlank;

import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Description("A controller for handling requests for hello messages")
public class SampleController {

	private final HelloWorldProperties helloWorldProperties;

	public SampleController(HelloWorldProperties helloWorldProperties) {
		this.helloWorldProperties = helloWorldProperties;
	}

	@GetMapping("/")
	@ResponseBody
	public Map<String, String> hello() {
		return Collections.singletonMap("message",
				"Hello " + this.helloWorldProperties.getName());
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
