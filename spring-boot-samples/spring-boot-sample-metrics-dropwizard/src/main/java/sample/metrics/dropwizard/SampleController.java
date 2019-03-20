/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.metrics.dropwizard;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Description("A controller for handling requests for hello messages")
public class SampleController {

	private final HelloWorldProperties helloWorldProperties;

	private final GaugeService gauges;

	public SampleController(HelloWorldProperties helloWorldProperties,
			GaugeService gauges) {
		this.helloWorldProperties = helloWorldProperties;
		this.gauges = gauges;
	}

	@GetMapping("/")
	@ResponseBody
	public Map<String, String> hello() {
		this.gauges.submit("timer.test.value", Math.random() * 1000 + 1000);
		return Collections.singletonMap("message",
				"Hello " + this.helloWorldProperties.getName());
	}

}
