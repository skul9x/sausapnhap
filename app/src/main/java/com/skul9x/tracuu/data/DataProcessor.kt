package com.skul9x.tracuu.data

import android.content.Context
import com.skul9x.tracuu.utils.DebugLogger
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
        DebugLogger.log("DataProcessor", "Bắt đầu tìm kiếm: '$query'")
        
        val rawData = getOrLoadData(context)
        
        if (rawData.isEmpty()) {
            DebugLogger.error("DataProcessor", "Dữ liệu raw trống! Không thể tìm kiếm.")
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
        
        DebugLogger.log("DataProcessor", "Kết quả tìm thô: ${filtered.size} dòng")
        val grouped = groupResults(filtered)
        DebugLogger.log("DataProcessor", "Kết quả sau khi gộp: ${grouped.size} nhóm")
        
        grouped
    }

    private fun getOrLoadData(context: Context): List<WardData> {
        if (cachedData != null) {
            return cachedData!!
        }
        DebugLogger.log("DataProcessor", "Dữ liệu chưa load, bắt đầu đọc file assets...")
        cachedData = loadRawData(context)
        return cachedData!!
    }

    private fun loadRawData(context: Context): List<WardData> {
        val dataList = mutableListOf<WardData>()
        try {
            val listFiles = context.assets.list("")
            DebugLogger.log("DataProcessor", "Files trong assets root: ${listFiles?.joinToString(", ")}")
            
            if (listFiles?.contains(DATA_FILE_NAME) != true) {
                DebugLogger.error("DataProcessor", "CRITICAL: Không tìm thấy file '$DATA_FILE_NAME' trong thư mục assets!")
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
            // Process last block
            if (currentBlock.isNotEmpty()) {
                processBlock(currentBlock)?.let { dataList.add(it) }
            }
            reader.close()
            DebugLogger.log("DataProcessor", "Đọc xong file. Tổng số dòng: $lineCount. Parse thành công: ${dataList.size} bản ghi.")
            
        } catch (e: Exception) {
            DebugLogger.error("DataProcessor", "Lỗi khi đọc file data", e)
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
        } else {
            // Log warning for malformed blocks if needed, but keep it quiet for now to avoid spam
            // DebugLogger.log("DataProcessor", "Block lỗi format: size=${block.size} content=$block")
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