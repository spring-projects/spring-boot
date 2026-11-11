/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.springbeansanddependencyinjection.singleconstructor;

import org.springframework.stereotype.Service;

@Service
public class MyAccountService implements AccountService {

	@SuppressWarnings("unused")
	private final RiskAssessor riskAssessor;

	public MyAccountService(RiskAssessor riskAssessor) {
		this.riskAssessor = riskAssessor;
	}

	// ...

}
