package com.skul9x.tracuu.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class WardData(
    val newProvince: String,
    val newWard: String,
    val oldWardDisplay: String,
    val oldWard: String,
    val oldDistrict: String,
    val oldProvince: String
)

data class GroupedResult(
    val newProvince: String,
    val newWard: String,
    val oldDistrict: String,
    val oldProvince: String,
    val oldUnits: List<OldUnitInfo>
)

data class OldUnitInfo(
    val name: String,
    val query: String
)

object DataProcessor {
    private const val DATA_FILE_NAME = "Tra cuu xa_processed.txt"
    private var cachedData: List<WardData>? = null

    suspend fun loadAndSearch(context: Context, query: String): List<GroupedResult> = withContext(Dispatchers.IO) {
        val rawData = getOrLoadData(context)
        
        if (rawData.isEmpty()) {
            return@withContext emptyList()
        }

        val filtered = if (query.isBlank()) {
            emptyList()
        } else {
            val normalizedQuery = query.lowercase(Locale.getDefault()).trim()
            rawData.filter { 
                it.newWard.lowercase(Locale.getDefault()).contains(normalizedQuery) 
            }
        }
        
        val grouped = groupResults(filtered)
        grouped
    }

    private fun getOrLoadData(context: Context): List<WardData> {
        if (cachedData != null) {
            return cachedData!!
        }
        cachedData = loadRawData(context)
        return cachedData!!
    }

    private fun loadRawData(context: Context): List<WardData> {
        val dataList = mutableListOf<WardData>()
        try {
            val listFiles = context.assets.list("")
            
            if (listFiles?.contains(DATA_FILE_NAME) != true) {
                return emptyList()
            }

            val inputStream = context.assets.open(DATA_FILE_NAME)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var currentBlock = mutableListOf<String>()
            var lineCount = 0
            
            reader.forEachLine { rawLine ->
                lineCount++
                var line = rawLine.trim()
                if (line.endsWith(",")) {
                    line = line.substring(0, line.length - 1)
                }
                
                val content = if (line.startsWith("\"") && line.endsWith("\"")) {
                    line.substring(1, line.length - 1)
                } else {
                    line
                }

                if (content.isEmpty()) {
                    if (currentBlock.isNotEmpty()) {
                        processBlock(currentBlock)?.let { dataList.add(it) }
                        currentBlock = mutableListOf()
                    }
                } else {
                    currentBlock.add(content)
                }
            }
            if (currentBlock.isNotEmpty()) {
                processBlock(currentBlock)?.let { dataList.add(it) }
            }
            reader.close()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dataList
    }

    private fun processBlock(block: List<String>): WardData? {
        if (block.size == 6) {
            if (block[0] == "TinhThanh_Moi" && block[1] == "PhuongXa_Moi") return null
            
            return WardData(
                newProvince = block[0],
                newWard = block[1],
                oldWardDisplay = block[2],
                oldWard = block[3],
                oldDistrict = block[4],
                oldProvince = block[5]
            )
        }
        return null
    }

    private fun groupResults(items: List<WardData>): List<GroupedResult> {
        val groupedMap = mutableMapOf<String, GroupedResult>()

        for (item in items) {
            val key = "${item.newProvince}|${item.oldProvince}|${item.oldDistrict}"
            val unitInfo = OldUnitInfo(
                name = item.oldWardDisplay,
                query = "${item.oldWardDisplay}, ${item.oldDistrict}, ${item.oldProvince}"
            )

            if (groupedMap.containsKey(key)) {
                val existing = groupedMap[key]!!
                val updatedList = existing.oldUnits + unitInfo
                groupedMap[key] = existing.copy(oldUnits = updatedList)
            } else {
                groupedMap[key] = GroupedResult(
                    newProvince = item.newProvince,
                    newWard = item.newWard,
                    oldDistrict = item.oldDistrict,
                    oldProvince = item.oldProvince,
                    oldUnits = listOf(unitInfo)
                )
            }
        }
        return groupedMap.values.toList()
    }
}