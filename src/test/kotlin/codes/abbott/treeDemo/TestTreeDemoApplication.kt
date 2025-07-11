package codes.abbott.treeDemo

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<TreeDemoApplication>().with(TestcontainersConfiguration::class).run(*args)
}
