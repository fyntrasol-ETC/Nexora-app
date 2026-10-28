package com.nexora_app.app.models

/**
 * Core vocabulary model — mirrors iOS Word.swift field-for-field so word
 * IDs stay identical across platforms if this ever syncs through Firestore.
 *
 * `difficulty` backs the Beginner / Intermediate / Advanced buttons on
 * StartLearningScreen. `status` drives which bucket a word shows up in on
 * WordsScreen. `isSaved` drives the saved-words filter independently of
 * both — a word can be saved regardless of whether it's known yet.
 *
 * Every word starts at Status.UNKNOWN — it only moves into KNOW or
 * DONT_KNOW once the person swipes or taps a flashcard.
 */
data class Word(
    val id: String,
    val text: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String = "",
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val status: Status = Status.UNKNOWN,
    val isSaved: Boolean = false
) {
    enum class Status {
        UNKNOWN, // not yet studied — every word starts here
        KNOW,
        DONT_KNOW
    }

    enum class Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED;

        val displayName: String
            get() = when (this) {
                BEGINNER -> "Beginner"
                INTERMEDIATE -> "Intermediate"
                ADVANCED -> "Advanced"
            }
    }

    companion object {
        // Seed data — real starter content the ViewModel's default
        // constructor relies on, same words/IDs as iOS's sampleWords.

        val beginnerWords: List<Word> = listOf(
            Word("b1", "happy", "adjective",
                "Feeling or showing pleasure or contentment.",
                "She felt happy when she saw her friends.", Difficulty.BEGINNER),
            Word("b2", "quick", "adjective",
                "Moving fast or done in a short time.",
                "He gave a quick answer to the question.", Difficulty.BEGINNER),
            Word("b3", "friendly", "adjective",
                "Kind and pleasant to be around.",
                "Our new neighbor is very friendly.", Difficulty.BEGINNER),
            Word("b4", "explain", "verb",
                "To make something clear by describing it.",
                "Can you explain how this works?", Difficulty.BEGINNER),
            Word("b5", "borrow", "verb",
                "To take something with the intention of returning it.",
                "I borrowed a pen from my classmate.", Difficulty.BEGINNER),
            Word("b6", "weather", "noun",
                "The state of the atmosphere at a place and time.",
                "The weather was sunny all week.", Difficulty.BEGINNER),
            Word("b7", "simple", "adjective",
                "Easy to understand or do.",
                "The instructions were simple to follow.", Difficulty.BEGINNER),
            Word("b8", "arrive", "verb",
                "To reach a place, especially at the end of a journey.",
                "We arrived at the airport early.", Difficulty.BEGINNER),
            Word("b9", "neighbor", "noun",
                "A person who lives near another.",
                "My neighbor waters my plants when I travel.", Difficulty.BEGINNER),
            Word("b10", "paperwork", "noun",
                "Documents that record or authorize something.",
                "I still have a stack of paperwork to finish before Friday.", Difficulty.BEGINNER)
        )

        val intermediateWords: List<Word> = listOf(
            Word("i1", "find one's way around", "verb",
                "To learn where to go without getting lost.",
                "After a few weeks, I was able to find my way around the new office.", Difficulty.INTERMEDIATE),
            Word("i2", "fit in", "verb",
                "To feel comfortable and accepted in a group or situation.",
                "It took a while, but she started to fit in with the team.", Difficulty.INTERMEDIATE),
            Word("i3", "persuade", "verb",
                "To convince someone to do or believe something.",
                "He tried to persuade his boss to approve the budget.", Difficulty.INTERMEDIATE),
            Word("i4", "reliable", "adjective",
                "Consistently good in quality; able to be trusted.",
                "She's a reliable colleague who always meets deadlines.", Difficulty.INTERMEDIATE),
            Word("i5", "postpone", "verb",
                "To delay an event to a later time.",
                "We had to postpone the meeting due to bad weather.", Difficulty.INTERMEDIATE),
            Word("i6", "negotiate", "verb",
                "To discuss something to reach an agreement.",
                "They negotiated a better price for the car.", Difficulty.INTERMEDIATE),
            Word("i7", "thorough", "adjective",
                "Complete, with attention to every detail.",
                "The inspector did a thorough review of the building.", Difficulty.INTERMEDIATE),
            Word("i8", "cope with", "verb",
                "To deal successfully with a difficult situation.",
                "It's hard to cope with so much stress at once.", Difficulty.INTERMEDIATE),
            Word("i9", "essential", "adjective",
                "Absolutely necessary.",
                "Sleep is essential for good health.", Difficulty.INTERMEDIATE),
            Word("i10", "accomplish", "verb",
                "To achieve or complete something successfully.",
                "She accomplished all her goals for the year.", Difficulty.INTERMEDIATE)
        )

        val advancedWords: List<Word> = listOf(
            Word("a1", "ambiguous", "adjective",
                "Open to more than one interpretation.",
                "The contract's wording was ambiguous and caused confusion.", Difficulty.ADVANCED),
            Word("a2", "meticulous", "adjective",
                "Showing great attention to detail.",
                "He's meticulous about checking his code before submitting it.", Difficulty.ADVANCED),
            Word("a3", "resilience", "noun",
                "The capacity to recover quickly from difficulties.",
                "Her resilience helped her bounce back after the setback.", Difficulty.ADVANCED),
            Word("a4", "procrastinate", "verb",
                "To delay doing something that should be done.",
                "He tends to procrastinate until the deadline is near.", Difficulty.ADVANCED),
            Word("a5", "ubiquitous", "adjective",
                "Present or found everywhere.",
                "Smartphones have become ubiquitous in modern life.", Difficulty.ADVANCED),
            Word("a6", "mitigate", "verb",
                "To make something less severe or harmful.",
                "The company took steps to mitigate the risk of data loss.", Difficulty.ADVANCED),
            Word("a7", "skeptical", "adjective",
                "Having doubts about something.",
                "She remained skeptical of the salesman's claims.", Difficulty.ADVANCED),
            Word("a8", "inevitable", "adjective",
                "Certain to happen; unavoidable.",
                "Change is inevitable in any growing company.", Difficulty.ADVANCED),
            Word("a9", "discrepancy", "noun",
                "A difference between things that should match.",
                "There's a discrepancy between the two reports.", Difficulty.ADVANCED),
            Word("a10", "articulate", "adjective",
                "Able to express ideas clearly and effectively.",
                "She gave an articulate presentation to the board.", Difficulty.ADVANCED)
        )
        val sampleWords: List<Word> = beginnerWords + intermediateWords + advancedWords
    }
}