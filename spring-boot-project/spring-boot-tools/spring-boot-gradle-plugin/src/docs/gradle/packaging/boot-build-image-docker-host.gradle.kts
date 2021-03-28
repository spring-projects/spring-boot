import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
	java
	id("org.springframework.boot") version "{gradle-project-version}"
}

tasks.getByName<BootJar>("bootJar") {
	mainClass.set("com.example.ExampleApplication")
}

// tag::docker-host[]
tasks.getByName<BootBuildImage>("bootBuildImage") {
	docker {
		host = "tcp://192.168.99.100:2376"
		isTlsVerify = true
		certPath = "/home/users/.minikube/certs"
	}
}
// end::docker-host[]

tasks.register("bootBuildImageDocker") {
	doFirst {
		println("host=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.host}")
		println("tlsVerify=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.isTlsVerify}")
		println("certPath=${tasks.getByName<BootBuildImage>("bootBuildImage").docker.certPath}")
	}
}
