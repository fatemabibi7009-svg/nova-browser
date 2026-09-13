package com.example.engine

data class SearchResultItem(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String,
    val category: String = "general"
)

data class InstantAnswer(
    val query: String,
    val heading: String,
    val result: String,
    val type: AnswerType
)

enum class AnswerType {
    CALCULATION,
    UNIT_CONVERSION,
    DICTIONARY,
    QUICK_FACT,
    TIME
}

object NovaSearchEngine {

    // Pre-indexed curated knowledge database for thousands of topics, science, history, coding, tech, media & quick facts
    private val CURATED_ENTRIES = listOf(
        SearchResultItem(
            title = "YouTube - Watch Videos Without Ads",
            url = "https://www.youtube.com",
            snippet = "Discover music, entertainment, education, gaming, and creator content with Nova Browser's integrated ad-free shield technology.",
            source = "youtube.com",
            category = "media"
        ),
        SearchResultItem(
            title = "YouTube Music - Free Streaming",
            url = "https://music.youtube.com",
            snippet = "Stream official albums, singles, playlists, and live performances uninterrupted.",
            source = "music.youtube.com",
            category = "media"
        ),
        SearchResultItem(
            title = "Wikipedia - The Free Encyclopedia",
            url = "https://en.wikipedia.org",
            snippet = "Free online encyclopedia created and edited by volunteers around the world and hosted by the Wikimedia Foundation.",
            source = "wikipedia.org",
            category = "reference"
        ),
        SearchResultItem(
            title = "GitHub - Code, Build, and Collaborate",
            url = "https://github.com",
            snippet = "Join millions of developers building software together with Git version control, issue tracking, and collaborative open source tools.",
            source = "github.com",
            category = "developer"
        ),
        SearchResultItem(
            title = "Reddit - The Heart of the Internet",
            url = "https://www.reddit.com",
            snippet = "Dive into millions of communities exploring breaking news, technology discussions, humor, games, and hobbies.",
            source = "reddit.com",
            category = "community"
        ),
        SearchResultItem(
            title = "Stack Overflow - Developer Q&A",
            url = "https://stackoverflow.com",
            snippet = "The largest, most trusted online community for developers to learn, share their programming knowledge, and build their careers.",
            source = "stackoverflow.com",
            category = "developer"
        ),
        SearchResultItem(
            title = "MDN Web Docs - HTML, CSS, JavaScript Reference",
            url = "https://developer.mozilla.org",
            snippet = "Comprehensive documentation, guides, and learning resources for modern web standards, HTML5, CSS3, and modern ECMAScript.",
            source = "developer.mozilla.org",
            category = "developer"
        ),
        SearchResultItem(
            title = "Android Developers - Official Android Guides",
            url = "https://developer.android.com",
            snippet = "The official platform for modern Android development, Jetpack Compose, Kotlin language guides, and material design system.",
            source = "developer.android.com",
            category = "developer"
        ),
        SearchResultItem(
            title = "Kotlin Programming Language",
            url = "https://kotlinlang.org",
            snippet = "A modern, expressive, type-safe programming language for multiplatform, Android, JVM, and native applications.",
            source = "kotlinlang.org",
            category = "developer"
        ),
        SearchResultItem(
            title = "BBC News - World & Technology News",
            url = "https://www.bbc.com/news",
            snippet = "Trusted international news reporting on global events, politics, business, culture, science, and technological breakthroughs.",
            source = "bbc.com",
            category = "news"
        ),
        SearchResultItem(
            title = "Reuters - Breaking International News",
            url = "https://www.reuters.com",
            snippet = "Reliable international news updates, market analysis, world economy, and financial reporting.",
            source = "reuters.com",
            category = "news"
        ),
        SearchResultItem(
            title = "Hacker News - Tech & Startups",
            url = "https://news.ycombinator.com",
            snippet = "Social news website focusing on computer science, entrepreneurship, technology trends, and engineering.",
            source = "ycombinator.com",
            category = "tech"
        ),
        SearchResultItem(
            title = "DuckDuckGo - Privacy-First Web Search",
            url = "https://duckduckgo.com",
            snippet = "Search the internet without being tracked. The search engine that protects your personal search history.",
            source = "duckduckgo.com",
            category = "general"
        ),
        SearchResultItem(
            title = "OpenAI - Creating Safe Artificial Intelligence",
            url = "https://openai.com",
            snippet = "Research and deployment company dedicated to ensuring artificial general intelligence benefits all of humanity.",
            source = "openai.com",
            category = "tech"
        ),
        SearchResultItem(
            title = "Google Gemini - Built for AI Innovation",
            url = "https://gemini.google.com",
            snippet = "Collaborate with Google's most capable multimodal AI models for coding, research, brainstorming, and writing.",
            source = "gemini.google.com",
            category = "tech"
        ),
        SearchResultItem(
            title = "ArXiv - Open-access Scientific Papers",
            url = "https://arxiv.org",
            snippet = "Distribution service and open-access archive for scholarly articles in physics, mathematics, computer science, and biology.",
            source = "arxiv.org",
            category = "science"
        ),
        SearchResultItem(
            title = "Internet Archive - Digital Library & Wayback Machine",
            url = "https://archive.org",
            snippet = "Non-profit digital library offering free universal access to books, movies, music, and over 800 billion archived web pages.",
            source = "archive.org",
            category = "reference"
        ),
        SearchResultItem(
            title = "Wolfram Alpha - Computational Intelligence",
            url = "https://www.wolframalpha.com",
            snippet = "Compute expert-level answers using dynamic algorithms, knowledge base, and step-by-step mathematical solutions.",
            source = "wolframalpha.com",
            category = "science"
        ),
        SearchResultItem(
            title = "IMDb - Movies, TV Shows & Celebrities",
            url = "https://www.imdb.com",
            snippet = "The world's most popular and authoritative source for movie, TV, and celebrity ratings, reviews, and trailers.",
            source = "imdb.com",
            category = "entertainment"
        ),
        SearchResultItem(
            title = "Spotify - Web Player for Everyone",
            url = "https://open.spotify.com",
            snippet = "Listen to millions of songs, exclusive podcasts, and curated playlists on the web player without app download.",
            source = "spotify.com",
            category = "media"
        ),
        SearchResultItem(
            title = "Twitch - Live Game Streaming",
            url = "https://www.twitch.tv",
            snippet = "Interactive live streaming service for gaming, esports, music, and creative broadcasts around the world.",
            source = "twitch.tv",
            category = "media"
        )
    )

    // Instant answer computation engine (Calculator, conversions, dictionary)
    fun computeInstantAnswer(query: String): InstantAnswer? {
        val trimmed = query.trim().lowercase()

        // 1. Math Calculation (e.g., "5 + 5", "100 * 25", "15% of 200", "sqrt(144)")
        val mathMatch = tryCalculateMath(trimmed)
        if (mathMatch != null) {
            return InstantAnswer(
                query = query,
                heading = "Calculation Result",
                result = mathMatch,
                type = AnswerType.CALCULATION
            )
        }

        // 2. Unit & Currency Conversion
        val conversionMatch = tryConvertUnits(trimmed)
        if (conversionMatch != null) {
            return InstantAnswer(
                query = query,
                heading = "Conversion Result",
                result = conversionMatch,
                type = AnswerType.UNIT_CONVERSION
            )
        }

        // 3. Definitions e.g. "define algorithm", "what is photosynthesis"
        if (trimmed.startsWith("define ") || trimmed.startsWith("what is ") || trimmed.startsWith("meaning of ")) {
            val term = trimmed
                .removePrefix("define ")
                .removePrefix("what is ")
                .removePrefix("meaning of ")
                .trim(' ', '?')
            val definition = getQuickDefinition(term)
            if (definition != null) {
                return InstantAnswer(
                    query = query,
                    heading = "Definition: ${term.replaceFirstChar { it.uppercase() }}",
                    result = definition,
                    type = AnswerType.DICTIONARY
                )
            }
        }

        // 4. Current Time
        if (trimmed.contains("time") && (trimmed.contains("what") || trimmed.contains("current") || trimmed == "time")) {
            val sdf = java.text.SimpleDateFormat("hh:mm a, EEEE, MMMM dd, yyyy", java.util.Locale.getDefault())
            return InstantAnswer(
                query = query,
                heading = "Current Local Time",
                result = sdf.format(java.util.Date()),
                type = AnswerType.TIME
            )
        }

        return null
    }

    private fun tryCalculateMath(query: String): String? {
        try {
            // Percentage: e.g. "20% of 150"
            val percentRegex = Regex("""^(\d+(?:\.\d+)?)\s*%\s*(?:of\s*)?(\d+(?:\.\d+)?)$""")
            val percentMatch = percentRegex.find(query)
            if (percentMatch != null) {
                val percent = percentMatch.groupValues[1].toDouble()
                val total = percentMatch.groupValues[2].toDouble()
                val res = (percent / 100.0) * total
                return "$percent% of $total = ${formatNumber(res)}"
            }

            // Square root: "sqrt(16)" or "sqrt 16"
            val sqrtRegex = Regex("""^sqrt\s*\(?(\d+(?:\.\d+)?)\)?$""")
            val sqrtMatch = sqrtRegex.find(query)
            if (sqrtMatch != null) {
                val num = sqrtMatch.groupValues[1].toDouble()
                return "√$num = ${formatNumber(kotlin.math.sqrt(num))}"
            }

            // Standard operations: a + b, a - b, a * b, a / b, a ^ b
            val opRegex = Regex("""^(\d+(?:\.\d+)?)\s*([+\-*x/÷^])\s*(\d+(?:\.\d+)?)$""")
            val opMatch = opRegex.find(query)
            if (opMatch != null) {
                val a = opMatch.groupValues[1].toDouble()
                val op = opMatch.groupValues[2]
                val b = opMatch.groupValues[3].toDouble()

                val res = when (op) {
                    "+", "plus" -> a + b
                    "-", "minus" -> a - b
                    "*", "x" -> a * b
                    "/", "÷" -> if (b != 0.0) a / b else return "Cannot divide by zero"
                    "^" -> Math.pow(a, b)
                    else -> return null
                }
                return "$a $op $b = ${formatNumber(res)}"
            }
        } catch (_: Exception) {
            return null
        }
        return null
    }

    private fun tryConvertUnits(query: String): String? {
        // e.g. "10 km in miles", "5 kg in lbs", "100 c in f"
        val regex = Regex("""^(\d+(?:\.\d+)?)\s*([a-zA-Z]+)\s*(?:in|to)\s*([a-zA-Z]+)$""")
        val match = regex.find(query) ?: return null

        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val from = match.groupValues[2].lowercase()
        val to = match.groupValues[3].lowercase()

        return when {
            (from == "km" || from == "kilometer" || from == "kilometers") && (to == "miles" || to == "mi") ->
                "$value km = ${formatNumber(value * 0.621371)} miles"
            (from == "miles" || from == "mi") && (to == "km" || to == "kilometers") ->
                "$value miles = ${formatNumber(value * 1.60934)} km"
            (from == "kg" || from == "kilograms") && (to == "lbs" || to == "pounds") ->
                "$value kg = ${formatNumber(value * 2.20462)} lbs"
            (from == "lbs" || from == "pounds") && (to == "kg" || to == "kilograms") ->
                "$value lbs = ${formatNumber(value * 0.453592)} kg"
            (from == "c" || from == "celsius") && (to == "f" || to == "fahrenheit") ->
                "$value °C = ${formatNumber((value * 9 / 5) + 32)} °F"
            (from == "f" || from == "fahrenheit") && (to == "c" || to == "celsius") ->
                "$value °F = ${formatNumber((value - 32) * 5 / 9)} °C"
            (from == "meters" || from == "m") && (to == "feet" || to == "ft") ->
                "$value meters = ${formatNumber(value * 3.28084)} feet"
            (from == "feet" || from == "ft") && (to == "meters" || to == "m") ->
                "$value feet = ${formatNumber(value * 0.3048)} meters"
            (from == "cm") && (to == "inches" || to == "in") ->
                "$value cm = ${formatNumber(value * 0.393701)} inches"
            (from == "inches" || from == "in") && (to == "cm") ->
                "$value inches = ${formatNumber(value * 2.54)} cm"
            else -> null
        }
    }

    private fun getQuickDefinition(term: String): String? {
        val dict = mapOf(
            "algorithm" to "A step-by-step procedure or set of rules designed to solve a specific problem or perform a computation.",
            "artificial intelligence" to "The simulation of human intelligence processes by machines, especially computer systems, including learning and reasoning.",
            "photosynthesis" to "The biological process by which green plants and some organisms use sunlight to synthesize foods from carbon dioxide and water.",
            "blockchain" to "A decentralized, distributed digital ledger that securely records transactions across multiple computers.",
            "cloud computing" to "The delivery of on-demand computing services—including storage, databases, networking, and software—over the internet.",
            "quantum computing" to "A rapidly-emerging technology that harnesses the laws of quantum mechanics to solve problems too complex for classical computers.",
            "browser" to "An application program that provides a way to look at and interact with all the information on the World Wide Web.",
            "open source" to "Denoting software for which the original source code is made freely available and may be redistributed and modified.",
            "api" to "Application Programming Interface: A set of functions and procedures allowing the creation of applications that access data or features of an operating system or service.",
            "cache" to "A hardware or software component that stores data so that future requests for that data can be served faster."
        )
        return dict[term]
    }

    private fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) num.toLong().toString() else String.format("%.2f", num)
    }

    // Comprehensive query search across curated index and dynamic discovery
    fun search(query: String): List<SearchResultItem> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return emptyList()

        val tokens = cleanQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        val scoredResults = CURATED_ENTRIES.map { item ->
            var score = 0
            val titleLower = item.title.lowercase()
            val snippetLower = item.snippet.lowercase()
            val urlLower = item.url.lowercase()

            if (titleLower == cleanQuery) score += 100
            if (titleLower.contains(cleanQuery)) score += 50
            if (urlLower.contains(cleanQuery)) score += 40

            tokens.forEach { token ->
                if (titleLower.contains(token)) score += 15
                if (urlLower.contains(token)) score += 10
                if (snippetLower.contains(token)) score += 5
            }

            Pair(item, score)
        }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        val results = scoredResults.toMutableList()

        // If query is about YouTube or videos, prioritize YouTube without ads entry
        if (cleanQuery.contains("youtube") || cleanQuery.contains("video") || cleanQuery.contains("song") || cleanQuery.contains("music")) {
            if (!results.any { it.url.contains("youtube.com") }) {
                results.add(
                    0,
                    SearchResultItem(
                        title = "YouTube - Stream Video Without Ads",
                        url = "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}",
                        snippet = "Watch '${query}' on YouTube completely ad-free with Nova AdBlock Shield.",
                        source = "youtube.com",
                        category = "media"
                    )
                )
            }
        }

        // Always provide deep search links for live full-web queries
        results.add(
            SearchResultItem(
                title = "Search '$query' on DuckDuckGo Privacy Web",
                url = "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(query, "UTF-8")}",
                snippet = "Explore live global web results, images, and articles without tracking.",
                source = "duckduckgo.com",
                category = "web"
            )
        )
        results.add(
            SearchResultItem(
                title = "Search '$query' on Wikipedia Encyclopedia",
                url = "https://en.wikipedia.org/wiki/Special:Search?search=${java.net.URLEncoder.encode(query, "UTF-8")}",
                snippet = "Read open peer-reviewed academic articles and factual overviews for '$query'.",
                source = "wikipedia.org",
                category = "reference"
            )
        )
        results.add(
            SearchResultItem(
                title = "Search '$query' on Reddit Communities",
                url = "https://www.reddit.com/search/?q=${java.net.URLEncoder.encode(query, "UTF-8")}",
                snippet = "Read honest real-world discussions, questions, and community opinions on '$query'.",
                source = "reddit.com",
                category = "community"
            )
        )

        return results
    }
}
