package com.saurav.pixelmusic.presentation.utils

import com.saurav.pixelmusic.R

object GenreIconProvider {
    
    // Provide a list of default common genres
    val DEFAULT_GENRES = listOf(
        "Rock", "Pop", "Jazz", "Classical", "Electronic", "Hip Hop",
        "Country", "Blues", "Reggae", "Metal", "Folk", "R&B", "Punk", "Indie",
        "Alternative", "Latino", "Reggaeton", "Salsa", "Bachata", "Merengue", "Cumbia",
        "Oldies", "Soundtrack", "Gaming", "Sleep", "Workout", "Party", "Focus"
    )

    // Map of name/keyword to resource ID for picking
    // We expose a list of "Selectable" icons.
    val SELECTABLE_ICONS = listOf(
        R.drawable.rock, R.drawable.pop_mic, R.drawable.sax, R.drawable.clasic_piano,
        R.drawable.electronic_sound, R.drawable.rapper, R.drawable.banjo, R.drawable.harmonica,
        R.drawable.maracas, R.drawable.metal_guitar, R.drawable.accordion, R.drawable.synth_piano,
        R.drawable.punk, R.drawable.idk_indie_ig, R.drawable.acoustic_guitar, R.drawable.alt_video,
        R.drawable.star_angle, R.drawable.conga, R.drawable.bongos, R.drawable.drum,
        R.drawable.rounded_schedule_24, R.drawable.rounded_tv_24, R.drawable.rounded_touch_app_24,
        R.drawable.rounded_alarm_24, R.drawable.rounded_celebration_24, R.drawable.rounded_edit_24,
        R.drawable.rounded_library_music_24,
        // Add some generic ones if available
        R.drawable.rounded_music_note_24, R.drawable.rounded_headphones_24, R.drawable.rounded_speaker_24
    )
    
    // Helper to get logic (accepts optional custom mapping)
    fun getGenreImageResource(genreId: String, customIcons: Map<String, Int> = emptyMap()): Any {
        // Check custom first
        customIcons[genreId]?.let { return it }
        
        return when (genreId.lowercase()) {
            "rock", "hard rock", "alternative rock", "classic rock" -> R.drawable.rock
            "pop", "pop rock", "k-pop", "dance pop" -> R.drawable.pop_mic
            "jazz", "smooth jazz", "bebop" -> R.drawable.sax
            "classical", "orchestra", "symphony", "piano" -> R.drawable.clasic_piano
            "electronic", "edm", "techno", "house", "trance", "dubstep", "electro" -> R.drawable.electronic_sound
            "hip hop", "hip-hop", "rap", "trap", "gangsta rap" -> R.drawable.rapper
            "country", "bluegrass", "americana" -> R.drawable.banjo
            "blues", "rhythm & blues" -> R.drawable.harmonica
            "reggae", "ska", "dancehall" -> R.drawable.maracas
            "metal", "heavy metal", "death metal", "black metal", "thrash metal" -> R.drawable.metal_guitar
            "folk", "acoustic", "singer-songwriter" -> R.drawable.accordion
            "r&b / soul", "rnb", "soul", "funk", "motown" -> R.drawable.synth_piano
            "punk", "punk rock", "pop punk", "grunge" -> R.drawable.punk
            "indie", "indie rock", "indie pop", "lo-fi" -> R.drawable.idk_indie_ig
            "folk & acoustic" -> R.drawable.acoustic_guitar
            "alternative", "alt-rock" -> R.drawable.alt_video
            "latino", "latin", "latin pop", "urbano latino" -> R.drawable.star_angle
            "reggaeton" -> R.drawable.rapper
            "salsa" -> R.drawable.conga
            "bachata" -> R.drawable.bongos
            "merengue" -> R.drawable.drum
            "cumbia" -> R.drawable.maracas
            "oldies", "retro", "80s", "90s" -> R.drawable.rounded_schedule_24
            "soundtrack", "score", "movie tunes" -> R.drawable.rounded_tv_24
            "gaming", "video game music" -> R.drawable.rounded_touch_app_24
            "sleep", "relax", "meditation", "ambient" -> R.drawable.rounded_alarm_24
            "workout", "gym", "fitness" -> R.drawable.electronic_sound
            "party", "club" -> R.drawable.rounded_celebration_24
            "focus", "study" -> R.drawable.rounded_edit_24
            "unknown" -> R.drawable.rounded_question_mark_24
            else -> R.drawable.rounded_library_music_24
        }
    }
}
