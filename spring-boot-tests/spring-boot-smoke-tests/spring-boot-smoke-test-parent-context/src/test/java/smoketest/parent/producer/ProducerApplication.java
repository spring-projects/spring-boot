/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.parent.producer;

import java.io.File;
import java.io.FileOutputStream;

import smoketest.parent.ServiceProperties;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceProperties.class)
public class ProducerApplication implements ApplicationRunner {

	private final ServiceProperties serviceProperties;

	public ProducerApplication(ServiceProperties serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		this.serviceProperties.getInputDir().mkdirs();
		if (!args.getNonOptionArgs().isEmpty()) {
			FileOutputStream stream = new FileOutputStream(
					new File(this.serviceProperties.getInputDir(), "data" + System.currentTimeMillis() + ".txt"));
			for (String arg : args.getNonOptionArgs()) {
				stream.write(arg.getBytes());
			}
			stream.flush();
			stream.close();
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(ProducerApplication.class, args);
	}

}
