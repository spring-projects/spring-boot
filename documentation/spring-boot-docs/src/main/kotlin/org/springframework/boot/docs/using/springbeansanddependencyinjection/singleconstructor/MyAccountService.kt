/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.using.springbeansanddependencyinjection.singleconstructor

import org.springframework.stereotype.Service

@Service
class MyAccountService(private val riskAssessor: RiskAssessor) : AccountService

