package codes.abbott.TreeDemo

import codes.abbott.TreeDemo.db.public.tables.pojos.Edge
import codes.abbott.TreeDemo.db.public.tables.records.EdgeRecord
import codes.abbott.TreeDemo.db.public.tables.references.EDGE
import jakarta.servlet.http.HttpServletRequest
import java.net.URI
import org.jooq.DSLContext
import org.jooq.Records
import org.jooq.Select
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL
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
		} catch (e: DuplicateKeyException) {
			throw ResponseStatusException(HttpStatus.CONFLICT, "Can't create edge: only one edge with toId ${to} may exist")
		} catch (e: DataAccessException) {
			throw ResponseStatusException(HttpStatus.CONFLICT, "Can't create edge; a trigger exception occurred: ${e.message}")
		}
		catch (e: Exception) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't create edge: ${e.javaClass.name} occurred,  ${e.message}")
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

	@GetMapping("/{root}/tree")
	fun buildTree(
		@PathVariable root: Long,
	): ResponseEntity<Node> {
		val rootExists = jooq.selectFrom(EDGE)
			.where(EDGE.FROM_ID.eq(root))
			.or(EDGE.TO_ID.eq(root))
			.limit(1)
			.fetchOne()
		if (rootExists == null) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "Node $root is not referenced")
		}

		// TODO: A near-repeat of the above. They can probably be merged, returning an Enum(FROM, TO, NULL) in that order
		val rootInPrimary = jooq.selectFrom(EDGE)
			.where(EDGE.TO_ID.eq(root))
			.limit(1)
			.fetchOne()
		/*
		toId is Edge's de facto primary key (needed for keyMapper, and fromId our parent relation (parentKeyMapper)
		Records.intoHierarchy only makes a new Record for a given key if it finds a row with that key (via keyMapper)
		NOT if it finds a key via parentKeyMapper. The absolute root of our tree virtually NEVER appears in the fromId
		column, so Records.intoHierarchy will never make a Node instance for it.
		In order to ensure the absolute root has a Node in our output, prepend a dummy row with its ID in toId
		IFF the request is actually asking to start with the absolute root
		 */
		var dummyRow: Select<EdgeRecord>
		if (rootInPrimary == null) { // this is the more common case
			dummyRow = jooq.select(
				DSL.inline(0).`as`(EDGE.FROM_ID), // 0 is assumedly never used so won't display
				DSL.inline(0).`as`(EDGE.TO_ID))
				.from(EDGE).coerce(EDGE)
				as Select<EdgeRecord> // coerce only gets us to ResultQuery<T> but union needs Select<T>
		} else {
			// if the requested root is NOT the absolute root, its ID exists in toId
			// Psql will complain if we violate the UNIQUE(toId) constraint by unioning in a dummy row (go figure)
			// so let's short-circuit that query to a SELECT WHEN false
			dummyRow = jooq.selectFrom(EDGE).where(DSL.inline(false))
		}

		val resultQuery = jooq.selectFrom(EDGE).union(dummyRow)

		// I estimate Records.intoHierarchy to run in O(1) time
		// This could be implemented as a stored SQL query with recursive CTEs, on a runtime optimized for data xforms
		// but I believe the query complexity may prevent in longer than linear time, and certainly be less readable
		val result = resultQuery
			.orderBy(EDGE.FROM_ID)
			.collect(Records.intoHierarchy(
				{ it.toId },
				{ if (it.toId == root) null else it.fromId },
				{ Node(it.toId) },
				{ parent, child -> parent.children.add(child) }))
			.first { it.id == root }

		if (result == null) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "No tree found")
		}

		return ResponseEntity.ok(result)
	}
}