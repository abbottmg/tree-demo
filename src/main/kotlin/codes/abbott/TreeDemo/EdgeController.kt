package codes.abbott.TreeDemo

import codes.abbott.TreeDemo.db.public.tables.pojos.Edge
import codes.abbott.TreeDemo.db.public.tables.records.EdgeRecord
import codes.abbott.TreeDemo.db.public.tables.references.EDGE
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/edge")
class EdgeController(
	@Autowired
	var jooq: DSLContext
) {

	@GetMapping("/")
	fun listAll(): List<Edge> {
		return jooq.selectFrom(EDGE).fetchInto(Edge::class.java)
	}

	@PostMapping("/{from}/{to}")
	fun createEdge(
        @PathVariable from: Long,
        @PathVariable to: Long
	) {
		val record = EdgeRecord(fromId = from, toId = to)
		jooq.insertInto(EDGE).set(record).execute()
	}
}