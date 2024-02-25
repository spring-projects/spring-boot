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

package smoketest.data.jdbc;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * SampleController class.
 */
@Controller
public class SampleController {

	private final CustomerRepository customerRepository;

	/**
	 * Constructs a new SampleController object with the specified CustomerRepository.
	 * @param customerRepository the CustomerRepository to be used by the SampleController
	 */
	public SampleController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	/**
	 * Retrieves a list of customers based on the provided name.
	 * @param name the name of the customers to retrieve
	 * @return a list of customers matching the provided name
	 */
	@GetMapping("/")
	@ResponseBody
	@Transactional(readOnly = true)
	public List<Customer> customers(@RequestParam String name) {
		return this.customerRepository.findByName(name);
	}

}
