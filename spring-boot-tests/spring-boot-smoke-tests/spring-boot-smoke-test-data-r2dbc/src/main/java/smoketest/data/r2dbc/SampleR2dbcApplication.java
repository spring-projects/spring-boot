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

package smoketest.data.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@SpringBootApplication
public class SampleR2dbcApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleR2dbcApplication.class, args);
	}

	@Bean
	public ApplicationRunner initializeDatabase(ConnectionFactory connectionFactory, ResourceLoader resourceLoader) {
		return (arguments) -> {
			Resource[] scripts = new Resource[] { resourceLoader.getResource("classpath:database-init.sql") };
			new ResourceDatabasePopulator(scripts).populate(connectionFactory).block();
		};

	}

}
