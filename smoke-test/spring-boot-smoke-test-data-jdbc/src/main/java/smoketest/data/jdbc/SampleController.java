/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jdbc;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController {

	private final CustomerRepository customerRepository;

	public SampleController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@GetMapping("/")
	@ResponseBody
	@Transactional(readOnly = true)
	public List<Customer> customers(@RequestParam String name) {
		return this.customerRepository.findByName(name);
	}

}
