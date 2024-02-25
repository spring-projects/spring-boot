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

package smoketest.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SampleCacheApplication class.
 */
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class SampleCacheApplication {

	/**
     * The main method is the entry point of the application.
     * It initializes a SpringApplicationBuilder and runs the application with the specified profiles.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
		new SpringApplicationBuilder().sources(SampleCacheApplication.class).profiles("app").run(args);
	}

}
