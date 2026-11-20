package com.example.engine

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

data class LexiconEntry(
    val word: String,
    val frequency: Int,
    val isCustom: Boolean = false
)

class TrieIndex {

    private class Node {
        val children = HashMap<Char, Node>(8)
        var isEnd: Boolean = false
        var word: String? = null
        var frequency: Int = 0
        var isCustom: Boolean = false
    }

    private val root = Node()
    private val lock = ReentrantReadWriteLock()
    private val wordMap = HashMap<String, LexiconEntry>(2048)

    fun insert(word: String, frequency: Int, isCustom: Boolean = false) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val lower = clean.lowercase()
        lock.write {
            val existing = wordMap[lower]
            val mergedFreq = if (existing != null) maxOf(existing.frequency, frequency) else frequency
            val mergedCustom = (existing?.isCustom == true) || isCustom
            val canonical = if (isCustom) clean else (existing?.word ?: clean)
            wordMap[lower] = LexiconEntry(canonical, mergedFreq, mergedCustom)

            var cur = root
            for (ch in lower) {
                cur = cur.children.getOrPut(ch) { Node() }
            }
            cur.isEnd = true
            cur.word = canonical
            cur.frequency = mergedFreq
            cur.isCustom = mergedCustom
        }
    }

    fun contains(word: String): Boolean = lock.read {
        wordMap.containsKey(word.lowercase())
    }

    fun getEntry(word: String): LexiconEntry? = lock.read {
        wordMap[word.lowercase()]
    }

    fun prefixCompletions(prefix: String, limit: Int = 6): List<LexiconEntry> {
        val lower = prefix.lowercase()
        if (lower.isEmpty()) return emptyList()
        return lock.read {
            var cur = root
            for (ch in lower) {
                cur = cur.children[ch] ?: return@read emptyList()
            }
            val collected = ArrayList<LexiconEntry>(32)
            dfs(cur, collected, maxCollect = 120)
            collected.sortWith(
                compareByDescending<LexiconEntry> { it.isCustom }
                    .thenByDescending { it.frequency }
                    .thenBy { it.word.length }
            )
            collected.take(limit)
        }
    }

    fun snapshotWords(): List<LexiconEntry> = lock.read {
        wordMap.values.toList()
    }

    fun size(): Int = lock.read { wordMap.size }

    private fun dfs(node: Node, out: MutableList<LexiconEntry>, maxCollect: Int) {
        if (out.size >= maxCollect) return
        if (node.isEnd && node.word != null) {
            out.add(LexiconEntry(node.word!!, node.frequency, node.isCustom))
        }
        for (child in node.children.values) {
            dfs(child, out, maxCollect)
            if (out.size >= maxCollect) return
        }
    }
}
