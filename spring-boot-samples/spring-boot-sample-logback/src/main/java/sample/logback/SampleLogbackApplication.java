/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.logback;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleLogbackApplication {

	private static final Logger logger = LoggerFactory
			.getLogger(SampleLogbackApplication.class);

	@PostConstruct
	public void logSomething() {
		logger.debug("Sample Debug Message");
		logger.trace("Sample Trace Message");
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleLogbackApplication.class, args);
	}

}
