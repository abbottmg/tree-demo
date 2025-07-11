package codes.abbott.treeDemo

import java.io.Serializable

data class Node(
    var id: Long? = null,
    var children: MutableList<Node> = mutableListOf(),
): Serializable
