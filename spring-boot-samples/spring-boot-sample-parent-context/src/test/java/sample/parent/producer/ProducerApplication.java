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

package sample.parent.producer;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProducerApplication implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		new File("target/input").mkdirs();
		if (args.length > 0) {
			FileOutputStream stream = new FileOutputStream("target/input/data"
					+ System.currentTimeMillis() + ".txt");
			for (String arg : args) {
				stream.write(arg.getBytes());
			}
			stream.flush();
			stream.close();
		}
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ProducerApplication.class, args);
	}

}
