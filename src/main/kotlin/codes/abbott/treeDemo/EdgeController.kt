package codes.abbott.treeDemo

import codes.abbott.treeDemo.db.public.tables.pojos.Edge
import codes.abbott.treeDemo.db.public.tables.records.EdgeRecord
import codes.abbott.treeDemo.db.public.tables.references.EDGE
import jakarta.servlet.http.HttpServletRequest
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.URI

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

	@GetMapping("/{from}/{to}")
	fun getOne(
        @PathVariable from: Long,
        @PathVariable to: Long
	): ResponseEntity<Edge> {
		val edge = jooq.selectFrom(EDGE)
			.where(EDGE.FROM_ID.eq(from))
			.and(EDGE.TO_ID.eq(to))
			.fetchOneInto(Edge::class.java)
		if (edge == null) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND)
		}
		return ResponseEntity.ok(edge)
	}

	@PostMapping("/{from}/{to}")
	fun createEdge(
        @PathVariable from: Long,
        @PathVariable to: Long,
        request: HttpServletRequest
	): ResponseEntity<Edge> {
		try {
			val record = EdgeRecord(fromId = from, toId = to)
			val inserted = jooq.insertInto(EDGE).set(record).returning().fetchOneInto(Edge::class.java)
			return ResponseEntity.created(URI.create(request.requestURI)).body(inserted!!)
		} catch (_: DuplicateKeyException) {
			throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Can't create edge: only one edge with toId $to may exist"
            )
		} catch (e: DataAccessException) {
			throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Can't create edge; a trigger exception occurred: ${e.message}"
            )
		}
		catch (e: Exception) {
			throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Can't create edge: ${e.javaClass.name} occurred,  ${e.message}"
            )
		}
	}

	@DeleteMapping("/{from}/{to}")
	fun deleteEdge(
        @PathVariable from: Long,
        @PathVariable to: Long
	): ResponseEntity<Void> {
		val e = EDGE
		val deleted = jooq.deleteFrom(e)
			.where(EDGE.FROM_ID.eq(from))
			.and(EDGE.TO_ID.eq(to))
			.returning()
			.fetchInto(Edge::class.java)
		if (deleted.isEmpty()) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "No edge found")
		}
		return ResponseEntity.noContent().build()
	}
}
