package org.springframework.boot.docs.io.grpc.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.docs.features.springapplication.MyApplication
import org.springframework.boot.runApplication
import org.springframework.grpc.client.ImportGrpcClients

@SpringBootApplication(proxyBeanMethods = false)
@ImportGrpcClients(target = "hello", types = [HelloWorldGrpc.HelloWorldBlockingStub::class])
class MyApplication

fun main(args: Array<String>) {
	runApplication<MyApplication>(*args)
}
