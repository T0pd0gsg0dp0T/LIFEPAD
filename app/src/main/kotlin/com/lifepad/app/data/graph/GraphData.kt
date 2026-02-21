package com.lifepad.app.data.graph

data class GraphNode(
    val id: String,
    val title: String,
    val type: NodeType,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f
)

enum class NodeType {
    NOTE,
    JOURNAL_ENTRY,
    TRANSACTION,
    HASHTAG
}

data class GraphEdge(
    val sourceId: String,
    val targetId: String
)

data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)
