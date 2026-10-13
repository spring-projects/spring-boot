/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.springbeansanddependencyinjection.multipleconstructors

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.PrintStream

@Service
class MyAccountService : AccountService {

	private val riskAssessor: RiskAssessor

	private val out: PrintStream

	@Autowired
	constructor(riskAssessor: RiskAssessor) {
		this.riskAssessor = riskAssessor
		out = System.out
	}

	constructor(riskAssessor: RiskAssessor, out: PrintStream) {
		this.riskAssessor = riskAssessor
		this.out = out
	}

	// ...

}

