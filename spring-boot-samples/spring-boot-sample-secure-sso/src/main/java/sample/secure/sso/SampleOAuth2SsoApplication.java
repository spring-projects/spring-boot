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
package sample.secure.sso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;

/**
 * After you launch the app it should prompt you for login with github. As long as it runs
 * on port 8080 on localhost it should work. Use an incognito window for testing
 * (otherwise you might find you are already authenticated).
 * 
 * @author Dave Syer
 */
@SpringBootApplication
@EnableOAuth2Sso
public class SampleOAuth2SsoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleOAuth2SsoApplication.class, args);
	}

}
