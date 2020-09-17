import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClassName = "com.example.ExampleApplication"
}

// tag::docker-host[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	docker {
		host = "tcp://192.168.99.100:2376"
		tlsVerify = true
		certPath = "/home/users/.minikube/certs"
	}
}
// end::docker-host[]
