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

package sample.data.jdbc;

import java.util.List;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController implements HealthIndicator {

	private final CustomerRepository customerRepository;

	public SampleController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	String n;

	@GetMapping("/")
	@ResponseBody
	@Transactional(readOnly = true)
	public List<Customer> customers(@RequestParam String name) {
		this.n = name;
		return this.customerRepository.findByName(name);
	}

	@Override
	public Health health() {
		int errorCode = check();
		if (errorCode != 200) {
			return Health.down().withDetail("Error Code", errorCode).build();
		}
		return Health.up().withDetail("Everything is OK", errorCode).build();
	}

	public int check() {
		if (this.customerRepository.findByName(this.n).isEmpty() | this.n == null) {
			return 500;
		}
		return 200;
	}

}
