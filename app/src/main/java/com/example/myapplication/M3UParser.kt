package com.example.myapplication

import java.util.regex.Pattern

class M3UParser {
    
    fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        if (content.isBlank()) return channels
        
        val lines = content.lines()
        var currentExtInf: String? = null
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            if (trimmedLine.startsWith("#EXTM3U")) {
                continue
            }
            
            if (trimmedLine.startsWith("#EXTINF:")) {
                currentExtInf = trimmedLine
            } else if (currentExtInf != null && trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                // This is a URL line
                val channel = parseExtInf(currentExtInf, trimmedLine)
                if (channel != null) {
                    channels.add(channel)
                }
                currentExtInf = null
            }
        }
        
        return channels
    }
    
    private fun parseExtInf(extInfLine: String, streamUrl: String): Channel? {
        try {
            // Parse #EXTINF:-1 tvg-id="id" tvg-name="name" tvg-logo="logo" group-title="group",Display Name
            var tvgId = ""
            var tvgName = ""
            var tvgLogo = ""
            var groupTitle = ""
            var displayName = ""
            
            // Extract display name (after comma)
            val commaIndex = extInfLine.indexOf(',')
            if (commaIndex != -1) {
                displayName = extInfLine.substring(commaIndex + 1).trim()
            }
            
            // Extract attributes (before comma)
            val attributesPart = if (commaIndex != -1) {
                extInfLine.substring(0, commaIndex)
            } else {
                extInfLine
            }
            
            // Parse attributes using regex
            val tvgIdPattern = Pattern.compile("tvg-id=\"([^\"]+)\"")
            val tvgNamePattern = Pattern.compile("tvg-name=\"([^\"]+)\"")
            val tvgLogoPattern = Pattern.compile("tvg-logo=\"([^\"]+)\"")
            val groupTitlePattern = Pattern.compile("group-title=\"([^\"]+)\"")
            
            val tvgIdMatcher = tvgIdPattern.matcher(attributesPart)
            if (tvgIdMatcher.find()) {
                tvgId = tvgIdMatcher.group(1)
            }
            
            val tvgNameMatcher = tvgNamePattern.matcher(attributesPart)
            if (tvgNameMatcher.find()) {
                tvgName = tvgNameMatcher.group(1)
            }
            
            val tvgLogoMatcher = tvgLogoPattern.matcher(attributesPart)
            if (tvgLogoMatcher.find()) {
                tvgLogo = tvgLogoMatcher.group(1)
            }
            
            val groupTitleMatcher = groupTitlePattern.matcher(attributesPart)
            if (groupTitleMatcher.find()) {
                groupTitle = groupTitleMatcher.group(1)
            }
            
            // Use display name if tvg-name is empty
            val finalName = if (tvgName.isNotEmpty()) tvgName else displayName
            val finalId = if (tvgId.isNotEmpty()) tvgId else finalName
            
            return Channel(
                id = finalId,
                name = finalName,
                streamUrl = streamUrl.trim(),
                logoUrl = tvgLogo,
                groupTitle = groupTitle
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
