/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.oauth2.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SampleReactiveOAuth2ClientApplication class.
 */
@SpringBootApplication
public class SampleReactiveOAuth2ClientApplication {

	/**
     * The main method is the entry point of the application.
     * It starts the Spring application by running the SampleReactiveOAuth2ClientApplication class.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
		SpringApplication.run(SampleReactiveOAuth2ClientApplication.class);
	}

}
