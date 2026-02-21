package com.lifepad.app.notepad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.graph.GraphEdge
import com.lifepad.app.data.graph.GraphNode
import com.lifepad.app.data.repository.GraphRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class GraphUiState(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val selectedNodeId: String? = null,
    val selectedNodeTitle: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val graphRepository: GraphRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        loadGraph()
    }

    private fun loadGraph() {
        viewModelScope.launch {
            val graphData = graphRepository.getGraphData()

            if (graphData.nodes.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    runForceDirectedLayout(graphData.nodes, graphData.edges)
                }
            }

            _uiState.update {
                it.copy(
                    nodes = graphData.nodes,
                    edges = graphData.edges,
                    isLoading = false
                )
            }
        }
    }

    private fun runForceDirectedLayout(nodes: List<GraphNode>, edges: List<GraphEdge>) {
        val area = 1000f
        val k = area / sqrt(nodes.size.toFloat().coerceAtLeast(1f))
        val iterations = 80
        var temperature = area / 10f

        // Initialize positions in circle
        val angleStep = (2 * Math.PI / nodes.size).toFloat()
        val cx = area / 2f
        val cy = area / 2f
        val radius = area / 3f
        nodes.forEachIndexed { i, node ->
            node.x = cx + radius * kotlin.math.cos(angleStep * i)
            node.y = cy + radius * kotlin.math.sin(angleStep * i)
        }

        val nodeMap = nodes.associateBy { it.id }

        for (iter in 0 until iterations) {
            // Reset velocities
            nodes.forEach { it.vx = 0f; it.vy = 0f }

            // Repulsion between all node pairs
            for (i in nodes.indices) {
                for (j in i + 1 until nodes.size) {
                    val n1 = nodes[i]
                    val n2 = nodes[j]
                    val dx = n1.x - n2.x
                    val dy = n1.y - n2.y
                    val dist = max(sqrt(dx * dx + dy * dy), 0.01f)
                    val force = (k * k) / dist
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    n1.vx += fx
                    n1.vy += fy
                    n2.vx -= fx
                    n2.vy -= fy
                }
            }

            // Attraction along edges
            for (edge in edges) {
                val source = nodeMap[edge.sourceId] ?: continue
                val target = nodeMap[edge.targetId] ?: continue
                val dx = target.x - source.x
                val dy = target.y - source.y
                val dist = max(sqrt(dx * dx + dy * dy), 0.01f)
                val force = (dist * dist) / k
                val fx = (dx / dist) * force
                val fy = (dy / dist) * force
                source.vx += fx
                source.vy += fy
                target.vx -= fx
                target.vy -= fy
            }

            // Apply velocities with temperature limiting
            for (node in nodes) {
                val disp = max(sqrt(node.vx * node.vx + node.vy * node.vy), 0.01f)
                val scale = min(disp, temperature) / disp
                node.x += node.vx * scale
                node.y += node.vy * scale
                // Clamp to bounds
                node.x = node.x.coerceIn(50f, area - 50f)
                node.y = node.y.coerceIn(50f, area - 50f)
            }

            temperature *= 0.95f
        }
    }

    fun onNodeSelected(nodeId: String?) {
        _uiState.update {
            val node = it.nodes.find { n -> n.id == nodeId }
            it.copy(
                selectedNodeId = nodeId,
                selectedNodeTitle = node?.title
            )
        }
    }
}

