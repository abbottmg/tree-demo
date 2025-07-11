package codes.abbott.treeDemo

import codes.abbott.treeDemo.db.public.tables.records.EdgeRecord
import codes.abbott.treeDemo.db.public.tables.references.EDGE
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.Records
import org.jooq.Select
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/node")
class NodeController(
    @Autowired
    var jooq: DSLContext
) {

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
        val dummyRow: Select<EdgeRecord> = if (rootInPrimary == null) { // this is the more common case
            jooq.select(
                DSL.inline(0).`as`(EDGE.FROM_ID), // 0 is assumedly never used so won't display
                DSL.inline(root).`as`(EDGE.TO_ID))
                .from(EDGE).coerce(EDGE)
                    as Select<EdgeRecord> // coerce only gets us to ResultQuery<T> but union needs Select<T>
        } else {
            // if the requested root is NOT the absolute root, its ID exists in toId
            // Psql will complain if we violate the UNIQUE(toId) constraint by unioning in a dummy row (go figure)
            // so let's short-circuit that query to a SELECT WHEN false
            jooq.selectFrom(EDGE).where(DSL.inline(false))
        }

        val resultQuery = jooq.selectFrom(EDGE).union(dummyRow)

        // I estimate Records.intoHierarchy to run in O(r) time, where r is all stored rows.
        // This could be implemented as a stored SQL query with recursive CTEs, on a runtime optimized for data xforms
        // but I believe the query complexity may prevent in longer than linear time, and certainly be less readable
        // NOTE: currently assumed that the table has only ONE noteworthy tree, so r is ~= n, where n tree.edges.count.
        // This query will build *all* trees in the table, extracting root's subtree if root is not the absolute root.
        // It chose this method to avoid the complexity of a depth-first recursive query but risks waste on other trees.
        val result = resultQuery
            .orderBy(EDGE.FROM_ID)
            .collect(
                Records.intoHierarchy(
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
