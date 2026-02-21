package com.lifepad.app.data.repository

import com.lifepad.app.data.graph.GraphData
import com.lifepad.app.data.graph.GraphEdge
import com.lifepad.app.data.graph.GraphNode
import com.lifepad.app.data.graph.NodeType
import com.lifepad.app.domain.parser.WikilinkParser
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphRepository @Inject constructor(
    private val noteRepository: NoteRepository,
    private val journalRepository: JournalRepository,
    private val financeRepository: FinanceRepository,
    private val hashtagRepository: HashtagRepository
) {
    suspend fun getGraphData(): GraphData {
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()
        val allHashtags = mutableSetOf<String>()

        // 1. Get Notes and their wikilinks
        val allNotes = noteRepository.getAllNotes().first()
        val noteByNormalizedTitle = allNotes.associateBy { it.title.lowercase() }
        val noteIds = allNotes.map { it.id }.toSet()
        allNotes.forEach { note ->
            val nodeId = "note_${note.id}"
            nodes.add(GraphNode(id = nodeId, title = note.title, type = NodeType.NOTE))

            // Wikilinks
            val outgoing = noteRepository.getOutgoingLinksForNote(note.id).first()
            outgoing.forEach { target ->
                if (target.id in noteIds) {
                    edges.add(GraphEdge(sourceId = nodeId, targetId = "note_${target.id}"))
                }
            }

            val hashtags = hashtagRepository.getHashtagsForNote(note.id)
            hashtags.forEach { hashtag ->
                allHashtags.add(hashtag.name)
                edges.add(GraphEdge(sourceId = nodeId, targetId = "hashtag_${hashtag.name}"))
            }
        }

        // 2. Get Journal Entries and their hashtags/wikilinks
        val allJournalEntries = journalRepository.getAllEntries().first()

        allJournalEntries.forEach { entry ->
            val entryNodeId = "entry_${entry.id}"
            nodes.add(GraphNode(id = entryNodeId, title = entry.content.take(30), type = NodeType.JOURNAL_ENTRY))

            val hashtags = hashtagRepository.getHashtagsForEntry(entry.id)
            hashtags.forEach { hashtag ->
                val hashtagNodeId = "hashtag_${hashtag.name}"
                allHashtags.add(hashtag.name)
                edges.add(GraphEdge(sourceId = entryNodeId, targetId = hashtagNodeId))
            }

            WikilinkParser.extractWikilinks(entry.content).forEach { targetTitle ->
                val targetNote = noteByNormalizedTitle[targetTitle.lowercase()] ?: return@forEach
                edges.add(GraphEdge(sourceId = entryNodeId, targetId = "note_${targetNote.id}"))
            }
        }

        // 3. Create nodes for all unique hashtags
        allHashtags.forEach { hashtagName ->
            nodes.add(GraphNode(id = "hashtag_$hashtagName", title = "#$hashtagName", type = NodeType.HASHTAG))
        }

        // 4. Get Transactions and their hashtags
        val allTransactions = financeRepository.getAllTransactions().first()
        allTransactions.forEach { transaction ->
            val transactionNodeId = "transaction_${transaction.id}"
            nodes.add(GraphNode(id = transactionNodeId, title = transaction.description, type = NodeType.TRANSACTION))

            val hashtags = hashtagRepository.getHashtagsForTransaction(transaction.id)
            hashtags.forEach { hashtag ->
                val hashtagNodeId = "hashtag_${hashtag.name}"
                allHashtags.add(hashtag.name)
                edges.add(GraphEdge(sourceId = transactionNodeId, targetId = hashtagNodeId))
            }
        }

        // Update hashtag nodes with all hashtags
        allHashtags.forEach { hashtagName ->
            if (nodes.find { it.id == "hashtag_$hashtagName" } == null) {
                nodes.add(GraphNode(id = "hashtag_$hashtagName", title = "#$hashtagName", type = NodeType.HASHTAG))
            }
        }

        return GraphData(nodes, edges)
    }
}
