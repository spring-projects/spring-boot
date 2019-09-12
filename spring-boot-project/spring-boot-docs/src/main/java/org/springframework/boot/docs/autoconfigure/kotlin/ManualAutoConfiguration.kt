package org.springframework.boot.docs.autoconfigure.kotlin

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.docs.configuration.kotlin.MyAnotherConfig
import org.springframework.boot.docs.configuration.kotlin.MyConfig
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Sample using individual components of [org.springframework.boot.autoconfigure.SpringBootApplication]
 *
 * @author Ibanga Enoobong Ime
 */
// tag::customizer[]
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(MyConfig::class, MyAnotherConfig::class)
class ManualApplication

fun main(args: Array<String>) {
	runApplication<ManualApplication>(*args)
}
// end::customizer[]
