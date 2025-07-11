package codes.abbott.treeDemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RestController
@SpringBootApplication
class TreeDemoApplication {

	@RequestMapping("/hello")
	fun hello(@RequestParam name: String = "World"): String {
		return String.format("Hello %s!", name)
	}

	@GetMapping("/")
	fun getBase(): String {
		return "Hello, base!"
	}
}
fun main(args: Array<String>) {
	runApplication<TreeDemoApplication>(*args)
}
