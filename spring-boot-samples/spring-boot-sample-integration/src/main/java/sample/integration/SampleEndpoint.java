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

package sample.integration;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.StreamUtils;

@MessageEndpoint
public class SampleEndpoint {

	private final HelloWorldService helloWorldService;

	public SampleEndpoint(HelloWorldService helloWorldService) {
		this.helloWorldService = helloWorldService;
	}

	@ServiceActivator
	public String hello(File input) throws Exception {
		FileInputStream in = new FileInputStream(input);
		String name = new String(StreamUtils.copyToByteArray(in));
		in.close();
		return this.helloWorldService.getHelloMessage(name);
	}

}
