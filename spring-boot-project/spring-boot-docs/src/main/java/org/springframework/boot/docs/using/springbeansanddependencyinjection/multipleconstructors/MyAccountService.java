/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.using.springbeansanddependencyinjection.multipleconstructors;

import java.io.PrintStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyAccountService implements AccountService {

	@SuppressWarnings("unused")
	private final RiskAssessor riskAssessor;

	@SuppressWarnings("unused")
	private final PrintStream out;

	@Autowired
	public MyAccountService(RiskAssessor riskAssessor) {
		this.riskAssessor = riskAssessor;
		this.out = System.out;
	}

	public MyAccountService(RiskAssessor riskAssessor, PrintStream out) {
		this.riskAssessor = riskAssessor;
		this.out = out;
	}

	// ...

}
