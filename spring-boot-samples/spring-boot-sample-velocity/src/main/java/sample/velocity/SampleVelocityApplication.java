/*
 * Copyright 2012-2014 the original author or authors.
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

package sample.velocity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.velocity.VelocityEngineUtils;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleVelocityApplication implements CommandLineRunner {

	@Value("${application.message}")
	private String message;

	@Autowired
	private VelocityEngine engine;

	@Override
	public void run(String... args) throws Exception {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("time", new Date());
		model.put("message", this.message);
		System.out.println(VelocityEngineUtils.mergeTemplateIntoString(this.engine,
				"welcome.vm", "UTF-8", model));
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleVelocityApplication.class, args);
	}

}
