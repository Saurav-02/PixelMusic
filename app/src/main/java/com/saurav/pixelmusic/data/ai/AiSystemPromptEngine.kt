package com.saurav.pixelmusic.data.ai


import javax.inject.Inject
import javax.inject.Singleton

enum class AiSystemPromptType {
    PLAYLIST,
    METADATA,
    TAGGING,
    MOOD_ANALYSIS,
    PERSONA,
    DAILY_MIX,
    GENERAL
}

@Singleton
class AiSystemPromptEngine @Inject constructor() {

    // Advanced prompt engineering: Enforcing structured output boundaries
    private val UNIVERSAL_CONSTRAINTS = """
        <constraints>
        - You are communicating with a programmatic parser, not a human.
        - Output ONLY the expected structure.
        - NO markdown formatting (e.g., do not wrap in ```json).
        - NO conversational filler, greetings, or explanations.
        - Any deviation will crash the application.
        </constraints>
    """.trimIndent()

    fun buildPrompt(basePersona: String, type: AiSystemPromptType, context: String = ""): String {
        val requirementLayer = when (type) {
            AiSystemPromptType.PLAYLIST, AiSystemPromptType.DAILY_MIX -> """
                <role>Intelligent music curation engine mapping user requests to cohesive playlists with natural flow.</role>
                <strategy>
                - Align selections with user prompt and taste patterns in <taste_telemetry>.
                - If request implies "discovery/new", prioritize the [DISCOVERY_POOL].
                - If request implies "favorites/familiar", heavily weight the [LISTENED] pool.
                - When the candidate pool has few matches for a niche vibe or specific genre/artist, supplement with up to 40% new online discoverable tracks.
                - Create a short, punchy playlist title of strictly 1 or 2 words (never a long phrase or subtitle, no colons or hyphens). Examples: "Pulse", "Night Drive", "Chill Vibe", "Morning Flow", "Study Beats", "Afterglow", "Serene Focus".
                - Provide a concise 1-2 sentence description matching the vibe.
                </strategy>
                <output_schema>
                Return ONLY a raw JSON object string with no markdown formatting or backticks:
                {"title":"Short Name","description":"Atmospheric vibe description.","tracks":[{"id":"id_from_pool"},{"online":"Artist - Song Title"}]}
                </output_schema>
            """.trimIndent()

            AiSystemPromptType.METADATA -> """
                <role>Precision music metadata specialist.</role>
                <strategy>
                - Fix spelling errors and standardizations in song titles and artists.
                - Replace generic genres ("Music", "Electronic") with highly specific subgenres ("Synthwave", "Nu-Disco").
                </strategy>
                <output_schema>
                Return ONLY a raw JSON object string.
                Format: {"title":"Clean Title", "artist":"Primary Artist", "album":"Album Name", "genre":"Specific Genre"}
                </output_schema>
            """.trimIndent()

            AiSystemPromptType.TAGGING -> """
                <role>Atmospheric audio tagging engine.</role>
                <strategy>
                - Generate exactly 6-10 highly descriptive, hyphenated acoustic tags.
                - Focus on mood, instrumentation, pace, and sonic texture.
                - All tags must be strictly lowercase.
                </strategy>
                <output_schema>
                Return ONLY a raw comma-separated text list.
                Format: cinematic, atmospheric-build, dark-synth, driving-beat
                </output_schema>
            """.trimIndent()

            AiSystemPromptType.MOOD_ANALYSIS -> """
                <role>Algorithmic audio sentiment analyzer.</role>
                <strategy>
                - Deduce structural properties from the given metadata.
                - Map confidence values from 0.0 to 1.0.
                - Primary moods: Joyful, Aggressive, Calm, Melancholic, Radiant, Intense, Somber.
                </strategy>
                <output_schema>
                Return ONLY the exact structured text format.
                Format: PrimaryMood | Energy:0.9 | Valence:0.1 | Danceability:0.4 | Acousticness:0.0
                </output_schema>
            """.trimIndent()

            AiSystemPromptType.PERSONA -> """
                <role>Daily Mix professional curator. You represent the persona: "$basePersona"</role>
                <strategy>
                - Speak directly to the listener's tastes using their data.
                - Maintain an enigmatic, sophisticated, and deeply empathetic tone.
                - Keep responses reasonably concise but beautifully written.
                - Do NOT use the universal programmatic constraints for persona responses; you are allowed to be conversational.
                </strategy>
            """.trimIndent()

            AiSystemPromptType.GENERAL -> """
                <role>PixelMusic Assistant</role>
                <strategy>
                Assist the user with any complex queries or actions inside their music ecosystem.
                </strategy>
            """.trimIndent()
        }

        val contextLayer = if (context.isNotBlank()) {
            """
            <user_context>
            $context
            </user_context>
            <legend>
            LISTENED Format: id|play_count|duration_mins|is_fav|metadata
            DISCOVERY Format: unplayed candidate tracks
            </legend>
            """.trimIndent()
        } else ""

        val systemBlock = """
            <system>
            <persona>$basePersona</persona>
            $requirementLayer
            </system>
        """.trimIndent()

        // Persona generation bypasses the strict JSON/raw constraints since it is meant to read as prose to the user
        return if (type == AiSystemPromptType.PERSONA || type == AiSystemPromptType.GENERAL) {
            listOf(systemBlock, contextLayer).filter { it.isNotBlank() }.joinToString("\n\n")
        } else {
            listOf(systemBlock, UNIVERSAL_CONSTRAINTS, contextLayer).filter { it.isNotBlank() }.joinToString("\n\n")
        }
    }
}
