package com.lifepad.app.notepad

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.lifepad.app.data.graph.GraphEdge
import com.lifepad.app.data.graph.GraphNode
import com.lifepad.app.data.graph.NodeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    onNavigateBack: () -> Unit,
    onNodeClick: (String, NodeType) -> Unit,
    viewModel: GraphViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Graph") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("nav_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("screen_graph"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.nodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("screen_graph"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No connections found yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use [[wikilinks]] in notes or #hashtags in any entry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("screen_graph")
            ) {
                GraphCanvas(
                    nodes = uiState.nodes,
                    edges = uiState.edges,
                    selectedNodeId = uiState.selectedNodeId,
                    onNodeTap = { viewModel.onNodeSelected(it) },
                    onNodeDoubleTap = { id, type -> onNodeClick(id, type) }
                )

                // Selected node info card
                uiState.selectedNodeId?.let { nodeId ->
                    uiState.nodes.find { it.id == nodeId }?.let { node ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onNodeClick(node.id, node.type) }
                                ) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    selectedNodeId: String?,
    onNodeTap: (String?) -> Unit,
    onNodeDoubleTap: (String, NodeType) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    val noteColor = MaterialTheme.colorScheme.primary
    val journalColor = MaterialTheme.colorScheme.secondary
    val transactionColor = MaterialTheme.colorScheme.error
    val hashtagColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale = (scale * zoom).coerceIn(0.3f, 5f)
                }
            }
            .pointerInput(nodes) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val hitNode = nodes.find { node ->
                            val screenX = node.x * scale + offsetX
                            val screenY = node.y * scale + offsetY
                            val dx = tapOffset.x - screenX
                            val dy = tapOffset.y - screenY
                            dx * dx + dy * dy < (30f * scale) * (30f * scale)
                        }
                        onNodeTap(hitNode?.id)
                    },
                    onDoubleTap = { tapOffset ->
                        val hitNode = nodes.find { node ->
                            val screenX = node.x * scale + offsetX
                            val screenY = node.y * scale + offsetY
                            val dx = tapOffset.x - screenX
                            val dy = tapOffset.y - screenY
                            dx * dx + dy * dy < (30f * scale) * (30f * scale)
                        }
                        hitNode?.let { onNodeDoubleTap(it.id, it.type) }
                    }
                )
            }
    ) {
        // Draw edges
        for (edge in edges) {
            val source = nodeMap[edge.sourceId] ?: continue
            val target = nodeMap[edge.targetId] ?: continue
            drawLine(
                color = outlineColor.copy(alpha = 0.5f),
                start = Offset(source.x * scale + offsetX, source.y * scale + offsetY),
                end = Offset(target.x * scale + offsetX, target.y * scale + offsetY),
                strokeWidth = 1.5f * scale
            )
        }

        // Draw nodes
        for (node in nodes) {
            val isSelected = node.id == selectedNodeId
            val nodeRadius = if (isSelected) 18f * scale else 12f * scale
            val color = when (node.type) {
                NodeType.NOTE -> noteColor
                NodeType.JOURNAL_ENTRY -> journalColor
                NodeType.TRANSACTION -> transactionColor
                NodeType.HASHTAG -> hashtagColor
            }

            drawCircle(
                color = color,
                radius = nodeRadius,
                center = Offset(node.x * scale + offsetX, node.y * scale + offsetY)
            )

            // Draw labels only when zoomed in enough
            if (scale > 0.6f) {
                drawContext.canvas.nativeCanvas.drawText(
                    node.title.take(20),
                    node.x * scale + offsetX,
                    node.y * scale + offsetY - nodeRadius - 6f,
                    Paint().apply {
                        textSize = 12f * scale
                        textAlign = Paint.Align.CENTER
                        this.color = onPrimaryColor.toArgb()
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}
