package smoketest.coroutines

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutinesApplication
fun main(args: Array<String>) {
	runApplication<CoroutinesApplication>(*args)
}
