package org.springframework.boot.docs.io.restclient.httpservice.groups.repeat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.service.registry.ImportHttpServices

@SpringBootApplication
@ImportHttpServices(group = "echo", types = [EchoService::class])
@ImportHttpServices(group = "other", types = [OtherService::class])
class MyApplication

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}

