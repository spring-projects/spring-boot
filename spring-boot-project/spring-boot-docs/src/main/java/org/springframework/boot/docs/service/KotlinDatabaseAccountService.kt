package org.springframework.boot.docs.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * * Sample showing autowiring the [org.springframework.beans.factory.annotation.Autowired] annotation
 */
// tag::customizer[]
@Service
class KotlinDatabaseAccountService @Autowired constructor(private val riskAssessor: RiskAssessor) : AccountService {
	//..
}

interface RiskAssessor

interface AccountService
// end::customizer[]
