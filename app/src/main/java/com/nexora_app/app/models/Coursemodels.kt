package com.nexora_app.app.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nexora_app.app.ui.theme.AppColors

// -----------------------------------------------------------------------
// Data layer for the Courses tab — ported 1:1 from iOS CourseContent.swift
// + PictureExercise.swift. A chapter's exercise is now one of two kinds
// (dialogue+quiz, or picture-vocab flashcards) via ChapterExerciseContent,
// same as iOS's ChapterExerciseContent enum.
//
// TODO: replace CourseCatalog.courses with real content fetched from the
// backend once this is off placeholder data (same TODO as iOS).
// -----------------------------------------------------------------------

// MARK: - Level

enum class CourseLevel(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

/**
 * Cycles the brand gradient stops across levels instead of picking a
 * color per-course, same pattern as iOS's CourseAccent.tint.
 */
enum class CourseAccent {
    INDIGO, TEAL, CORAL;

    val tint: Color
        @Composable get() = when (this) {
            INDIGO -> AppColors.accentVideos   // blue — brand gradient start
            TEAL -> AppColors.accentCourses    // purple — brand gradient mid
            CORAL -> AppColors.accentProfile   // pink — brand gradient end
        }
}

// MARK: - Course / Chapter

data class Course(
    val id: String,
    val title: String,
    /** One or two lines shown above the chapter list on the course detail screen. */
    val description: String,
    val level: CourseLevel,
    val accent: CourseAccent,
    /** Short "what you'll learn" tags — keep each 1-2 words. */
    val highlights: List<String>,
    val chapters: List<Chapter>
)

data class Chapter(
    val number: Int,
    val title: String,
    /** Shown on the chapter's own title/intro page before the exercise starts. */
    val description: String,
    val exercise: ChapterExerciseContent
)

// MARK: - Exercise (dialogue + quiz)

data class Exercise(
    /** e.g. "Dialogue Exercise" — heading on the exercise's own intro page. */
    val title: String,
    /** The 1-2 line explanation shown under the title on that intro page. */
    val instructions: String,
    val dialogue: List<DialogueLine>,
    val questions: List<QuizQuestion>
)

enum class DialogueSpeaker(val displayName: String) {
    MIA("Mia"),
    LEO("Leo")
}

data class DialogueLine(
    val speaker: DialogueSpeaker,
    val text: String
)

enum class QuizQuestionType {
    MULTIPLE_CHOICE,
    FILL_IN_BLANK
}

data class QuizQuestion(
    val type: QuizQuestionType,
    /** For FILL_IN_BLANK, write the prompt with "___" where the blank goes. */
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

// MARK: - Exercise (picture vocab)
//
// Second exercise type, alternated with dialogue exercises per chapter —
// ported 1:1 from iOS's PictureExercise.swift. A learner sees an image,
// types their guess, then flips the card to reveal the correct word +
// an example sentence.

data class PictureCard(
    val id: String,
    /**
     * Drawable resource name (res/drawable), same "drop it in, done" flow
     * as any other image asset. NOTE: unlike iOS asset-catalog names,
     * Android resource names can only contain lowercase letters, digits,
     * and underscores — no hyphens (iOS's "wine-glass" becomes
     * "wine_glass" here). If the drawable is missing, PictureExerciseScreen
     * falls back to a photo placeholder instead of crashing.
     */
    val imageName: String,
    /** Exact word/phrase to guess, e.g. "umbrella". Matching is
     *  case-insensitive and whitespace-trimmed (see PictureExerciseController). */
    val answer: String,
    /** Shown on the reveal face under the answer for context. */
    val exampleSentence: String
)

data class PictureExercise(
    /** e.g. "Picture Vocabulary" — heading on the exercise's intro page. */
    val title: String,
    val instructions: String,
    val cards: List<PictureCard>
)

// MARK: - Chapter exercise kind
//
// Mirrors iOS's ChapterExerciseContent enum. CoursesScreen's `when` on
// CourseRoute.Exercise switches on this to push the right screen
// (DialogueScreen vs PictureExerciseScreen).

sealed class ChapterExerciseContent {
    data class Dialogue(val exercise: Exercise) : ChapterExerciseContent()
    data class Picture(val exercise: PictureExercise) : ChapterExerciseContent()

    val title: String
        get() = when (this) {
            is Dialogue -> exercise.title
            is Picture -> exercise.title
        }

    val instructions: String
        get() = when (this) {
            is Dialogue -> exercise.instructions
            is Picture -> exercise.instructions
        }
}

// MARK: - Tiny builders (keep the catalog below readable, mirrors the Swift file's terseness)

private fun mia(text: String) = DialogueLine(DialogueSpeaker.MIA, text)
private fun leo(text: String) = DialogueLine(DialogueSpeaker.LEO, text)
private fun mcq(prompt: String, options: List<String>, correct: Int) =
    QuizQuestion(QuizQuestionType.MULTIPLE_CHOICE, prompt, options, correct)
private fun fib(prompt: String, options: List<String>, correct: Int) =
    QuizQuestion(QuizQuestionType.FILL_IN_BLANK, prompt, options, correct)

private const val DEFAULT_EXERCISE_TITLE = "Dialogue Exercise"
private const val DEFAULT_EXERCISE_INSTRUCTIONS =
    "Read the dialogue below, then answer questions about it from memory."
private const val DEFAULT_PICTURE_TITLE = "Picture Vocabulary"
private const val DEFAULT_PICTURE_INSTRUCTIONS =
    "Study the picture, type your guess, then reveal the answer."

private fun dialogueExercise(
    dialogue: List<DialogueLine>,
    questions: List<QuizQuestion>
): ChapterExerciseContent = ChapterExerciseContent.Dialogue(
    Exercise(
        title = DEFAULT_EXERCISE_TITLE,
        instructions = DEFAULT_EXERCISE_INSTRUCTIONS,
        dialogue = dialogue,
        questions = questions
    )
)

private fun pictureExercise(cards: List<PictureCard>): ChapterExerciseContent =
    ChapterExerciseContent.Picture(
        PictureExercise(
            title = DEFAULT_PICTURE_TITLE,
            instructions = DEFAULT_PICTURE_INSTRUCTIONS,
            cards = cards
        )
    )

// MARK: - Sample catalog

object CourseCatalog {

    fun courseById(id: String): Course? = courses.firstOrNull { it.id == id }

    val courses: List<Course> by lazy {
        listOf(
            beginner, beginnerSmallTalk, beginnerDirections, beginnerWeather, beginnerFamily,
            intermediate, intermediatePlans, intermediatePhone, intermediateShopping, intermediateDoctor,
            advanced, advancedNegotiating, advancedPresentation, advancedComplaints, advancedDebate
        )
    }

    fun coursesFor(level: CourseLevel): List<Course> = courses.filter { it.level == level }

    // MARK: Beginner — Everyday Greetings

    val beginner = Course(
        id = "beginner-greetings",
        title = "Everyday Greetings",
        description = "Learn how to greet people, introduce yourself, and keep a friendly conversation going.",
        level = CourseLevel.BEGINNER,
        accent = CourseAccent.INDIGO,
        highlights = listOf("Greetings", "Small talk", "Introductions"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Saying Hello",
                description = "Learn how to greet people and start a simple conversation.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Hi Leo! How are you today?"),
                        leo("I'm doing great, thanks! How about you?"),
                        mia("I'm good too. Have you met our new neighbor?"),
                        leo("Not yet. What's her name?"),
                        mia("Her name is Sarah. She just moved in yesterday."),
                        leo("Nice! I should go say hello to her."),
                        mia("That's a great idea. She seems very friendly."),
                        leo("Do you know where she's from?"),
                        mia("I think she mentioned she's from Canada."),
                        leo("Cool! I'll ask her more about it later."),
                        mia("Sounds good. See you at lunch, Leo!"),
                        leo("See you then, Mia. Have a great morning!")
                    ),
                    questions = listOf(
                        mcq("How is Leo doing today?", listOf("Terrible", "Great", "Tired", "Busy"), 1),
                        mcq("What is the new neighbor's name?", listOf("Sarah", "Mia", "Leo", "Anna"), 0),
                        fib("Sarah just ___ in yesterday.", listOf("moved", "left", "called", "visited"), 0),
                        mcq("Where is Sarah from?", listOf("Canada", "France", "Japan", "Mexico"), 0),
                        fib("Leo will ask Sarah more about it ___.", listOf("later", "never", "yesterday", "now"), 0),
                        mcq("When will Mia and Leo see each other again?", listOf("At lunch", "Tomorrow", "Never", "At dinner"), 0)
                    )
                )
            ),
            Chapter(
                number = 2,
                title = "Around the House",
                description = "Learn everyday household object names with picture flashcards.",
                exercise = pictureExercise(
                    cards = listOf(
                        PictureCard("beginner-greetings-ch2-card1", "umbrella", "umbrella", "Take an umbrella, it might rain later."),
                        PictureCard("beginner-greetings-ch2-card2", "clock", "clock", "The clock on the wall says three o'clock."),
                        PictureCard("beginner-greetings-ch2-card3", "chair", "chair", "Please pull up a chair and sit down."),
                        PictureCard("beginner-greetings-ch2-card4", "mug", "mug", "She poured coffee into her favorite mug."),
                        PictureCard("beginner-greetings-ch2-card5", "lamp", "lamp", "Turn on the lamp, it's getting dark.")
                    )
                )
            ),
            Chapter(
                number = 3,
                title = "Making Small Talk",
                description = "Learn common small talk phrases about the weather and weekend plans.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Nice weather today, isn't it?"),
                        leo("Yes, it's really sunny! Perfect for a walk."),
                        mia("Do you have any plans for the weekend?"),
                        leo("I'm going hiking with some friends. What about you?"),
                        mia("I'm just going to relax at home."),
                        leo("That sounds nice too. Enjoy your weekend, Mia!")
                    ),
                    questions = listOf(
                        mcq("What is the weather like today?", listOf("Sunny", "Rainy", "Snowy", "Windy"), 0),
                        mcq("What is Leo doing this weekend?", listOf("Hiking", "Working", "Sleeping", "Studying"), 0),
                        fib("Mia is going to ___ at home.", listOf("relax", "cook", "clean", "paint"), 0)
                    )
                )
            )
        )
    )

    // MARK: Beginner extras

    val beginnerSmallTalk = Course(
        id = "beginner-small-talk",
        title = "Making Small Talk",
        description = "Pick up easy openers for chatting about everyday topics with someone new.",
        level = CourseLevel.BEGINNER,
        accent = CourseAccent.INDIGO,
        highlights = listOf("Icebreakers", "New people"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Breaking the Ice",
                description = "Learn easy openers for chatting with someone new.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Hi, I don't think we've met before."),
                        leo("No, I'm new here. I'm Leo."),
                        mia("Welcome, Leo! How do you like it so far?"),
                        leo("Everyone's been really friendly. I like it a lot.")
                    ),
                    questions = listOf(
                        mcq("Is Leo new here?", listOf("Yes", "No", "Not sure", "He's leaving"), 0),
                        fib("Leo says everyone has been really ___.", listOf("friendly", "busy", "quiet", "strict"), 0)
                    )
                )
            )
        )
    )

    val beginnerDirections = Course(
        id = "beginner-directions",
        title = "Asking for Directions",
        description = "Learn how to ask for and follow directions when you're finding your way.",
        level = CourseLevel.BEGINNER,
        accent = CourseAccent.INDIGO,
        highlights = listOf("Directions", "Locations"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Finding Your Way",
                description = "Practice asking for and giving simple directions.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Excuse me, how do I get to the train station?"),
                        leo("Go straight two blocks, then turn left."),
                        mia("Straight, then left. Is it far?"),
                        leo("No, it's only a five-minute walk.")
                    ),
                    questions = listOf(
                        mcq("Where does Mia want to go?", listOf("Train station", "Bus stop", "Airport", "Hotel"), 0),
                        fib("It's only a ___-minute walk.", listOf("five", "twenty", "one", "sixty"), 0)
                    )
                )
            )
        )
    )

    val beginnerWeather = Course(
        id = "beginner-weather",
        title = "Talking About the Weather",
        description = "Practice the go-to phrases for describing what it's like outside.",
        level = CourseLevel.BEGINNER,
        accent = CourseAccent.INDIGO,
        highlights = listOf("Weather talk", "Descriptions"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Hot or Cold",
                description = "Learn phrases for describing today's weather.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("It's freezing out today!"),
                        leo("I know, I should have worn a warmer jacket."),
                        mia("They said it might snow later."),
                        leo("Really? I hope it doesn't stick to the roads.")
                    ),
                    questions = listOf(
                        mcq("How does Mia describe the weather?", listOf("Freezing", "Hot", "Rainy", "Windy"), 0),
                        fib("It might ___ later, they said.", listOf("snow", "clear up", "get hot", "flood"), 0)
                    )
                )
            )
        )
    )

    val beginnerFamily = Course(
        id = "beginner-family",
        title = "Talking About Family",
        description = "Learn how to talk about your family members and ask about someone else's.",
        level = CourseLevel.BEGINNER,
        accent = CourseAccent.INDIGO,
        highlights = listOf("Family", "Describing people"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Meet the Family",
                description = "Learn how to describe your family members.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Do you have any brothers or sisters, Leo?"),
                        leo("I have one younger sister. What about you?"),
                        mia("I have two older brothers."),
                        leo("That sounds like a lively house growing up!")
                    ),
                    questions = listOf(
                        mcq("How many siblings does Leo have?", listOf("One", "Two", "Three", "None"), 0),
                        fib("Mia has two ___ brothers.", listOf("older", "younger", "twin", "step"), 0)
                    )
                )
            )
        )
    )

    // MARK: Intermediate — At the Restaurant

    val intermediate = Course(
        id = "intermediate-restaurant",
        title = "At the Restaurant",
        description = "Order food with confidence, ask about the menu, and handle the bill like a regular.",
        level = CourseLevel.INTERMEDIATE,
        accent = CourseAccent.TEAL,
        highlights = listOf("Ordering", "Menu questions", "Paying"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Ordering Food",
                description = "Learn phrases for ordering food and drinks at a restaurant.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Good evening! Table for two, please."),
                        leo("Of course, right this way. Here are your menus."),
                        mia("Thank you. Could we have a few minutes to decide?"),
                        leo("Take your time. I'll come back shortly."),
                        mia("I think I'm ready to order now."),
                        leo("Great, what would you like to have?"),
                        mia("I'll have the grilled salmon with a side salad."),
                        leo("Excellent choice. Anything to drink?"),
                        mia("Just a glass of water, please."),
                        leo("Sure thing. I'll put your order in right away."),
                        mia("Thank you so much!"),
                        leo("You're welcome. Your food will be out shortly.")
                    ),
                    questions = listOf(
                        mcq("How many people are in Mia's party?", listOf("Two", "Three", "One", "Four"), 0),
                        fib("The waiter says, \"Take your ___.\"", listOf("time", "order", "seat", "menu"), 0),
                        mcq("What did Mia order to eat?", listOf("Grilled salmon", "Steak", "Pasta", "Pizza"), 0),
                        mcq("What did Mia order to drink?", listOf("Water", "Soda", "Juice", "Coffee"), 0),
                        fib("The waiter will put the order in ___ away.", listOf("right", "some", "far", "never"), 0),
                        mcq("What does Mia say at the end?", listOf("Thank you so much", "Goodbye", "See you later", "No thanks"), 0)
                    )
                )
            ),
            Chapter(
                number = 2,
                title = "On the Menu",
                description = "Learn common restaurant menu items and table settings with picture flashcards.",
                exercise = pictureExercise(
                    cards = listOf(
                        PictureCard("intermediate-restaurant-ch2-card1", "menu", "menu", "The waiter handed us each a menu."),
                        PictureCard("intermediate-restaurant-ch2-card2", "fork", "fork", "She picked up her fork and started eating."),
                        PictureCard("intermediate-restaurant-ch2-card3", "wine_glass", "wine glass", "He filled the wine glass halfway."),
                        PictureCard("intermediate-restaurant-ch2-card4", "napkin", "napkin", "She wiped her hands on a napkin."),
                        PictureCard("intermediate-restaurant-ch2-card5", "dessert", "dessert", "We shared a slice of chocolate dessert.")
                    )
                )
            ),
            Chapter(
                number = 3,
                title = "Paying the Bill",
                description = "Learn how to ask for the bill and split payment.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Could we have the bill, please?"),
                        leo("Of course, I'll bring it right over."),
                        mia("Can we split it between two cards?"),
                        leo("Absolutely, that's no problem at all."),
                        mia("Great, thank you for a lovely dinner."),
                        leo("You're very welcome. Have a great night!")
                    ),
                    questions = listOf(
                        mcq("What does Mia ask for?", listOf("The bill", "More food", "A refund", "A table"), 0),
                        mcq("How does Mia want to pay?", listOf("Split between two cards", "Cash only", "One card", "Gift card"), 0),
                        fib("Leo says, \"Have a great ___!\"", listOf("night", "day", "trip", "meal"), 0)
                    )
                )
            )
        )
    )

    // MARK: Intermediate extras

    val intermediatePlans = Course(
        id = "intermediate-plans",
        title = "Making Plans",
        description = "Learn how to suggest, confirm, and adjust plans with a friend.",
        level = CourseLevel.INTERMEDIATE,
        accent = CourseAccent.TEAL,
        highlights = listOf("Making plans", "Scheduling"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Setting a Time",
                description = "Practice suggesting and confirming plans with a friend.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Are you free this Saturday?"),
                        leo("I think so. What did you have in mind?"),
                        mia("Maybe we could catch a movie in the afternoon."),
                        leo("Sounds great, let's say 3 PM?")
                    ),
                    questions = listOf(
                        mcq("What day are they planning for?", listOf("Saturday", "Sunday", "Friday", "Monday"), 0),
                        fib("They agree to meet at ___ PM.", listOf("3", "5", "8", "12"), 0)
                    )
                )
            )
        )
    )

    val intermediatePhone = Course(
        id = "intermediate-phone",
        title = "Talking on the Phone",
        description = "Practice everyday phone etiquette, from answering to taking a message.",
        level = CourseLevel.INTERMEDIATE,
        accent = CourseAccent.TEAL,
        highlights = listOf("Phone calls", "Messages"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Taking a Message",
                description = "Learn phrases for phone calls and leaving messages.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Hello, is Leo available?"),
                        leo("This is Leo speaking."),
                        mia("Great, I'm calling about tomorrow's meeting."),
                        leo("Sure, let me grab a pen to take notes.")
                    ),
                    questions = listOf(
                        mcq("Who answers the phone?", listOf("Leo", "Mia", "A receptionist", "No one"), 0),
                        fib("Mia is calling about tomorrow's ___.", listOf("meeting", "party", "flight", "lunch"), 0)
                    )
                )
            )
        )
    )

    val intermediateShopping = Course(
        id = "intermediate-shopping",
        title = "Shopping for Clothes",
        description = "Learn how to ask about sizes, colors, and where to try things on.",
        level = CourseLevel.INTERMEDIATE,
        accent = CourseAccent.TEAL,
        highlights = listOf("Shopping", "Sizes & fit"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Finding the Right Size",
                description = "Practice asking about sizes, colors, and trying things on.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Do you have this jacket in a medium?"),
                        leo("Let me check. Yes, here you go."),
                        mia("Perfect, is there a fitting room nearby?"),
                        leo("Right this way, second door on the left.")
                    ),
                    questions = listOf(
                        mcq("What size does Mia ask for?", listOf("Medium", "Small", "Large", "Extra large"), 0),
                        fib("The fitting room is the second door on the ___.", listOf("left", "right", "back", "front"), 0)
                    )
                )
            )
        )
    )

    val intermediateDoctor = Course(
        id = "intermediate-doctor",
        title = "At the Doctor's Office",
        description = "Learn how to describe symptoms and understand a doctor's questions.",
        level = CourseLevel.INTERMEDIATE,
        accent = CourseAccent.TEAL,
        highlights = listOf("Symptoms", "Health"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Describing Symptoms",
                description = "Learn how to describe how you're feeling to a doctor.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("What seems to be the problem today?"),
                        leo("I've had a sore throat since Monday."),
                        mia("Any fever or trouble swallowing?"),
                        leo("A mild fever, but no trouble swallowing.")
                    ),
                    questions = listOf(
                        mcq("What is Leo's main symptom?", listOf("Sore throat", "Headache", "Back pain", "Cough"), 0),
                        fib("Leo has had it since ___.", listOf("Monday", "Friday", "yesterday", "last month"), 0)
                    )
                )
            )
        )
    )

    // MARK: Advanced — Job Interview Skills

    val advanced = Course(
        id = "advanced-interview",
        title = "Job Interview Skills",
        description = "Prepare to answer common interview questions clearly and confidently.",
        level = CourseLevel.ADVANCED,
        accent = CourseAccent.CORAL,
        highlights = listOf("Interviews", "Strengths", "Follow-up Qs"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Answering Common Questions",
                description = "Practice answering typical interview questions with confidence.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Thanks for coming in today, Leo. Tell me about yourself."),
                        leo("Sure. I have five years of experience in marketing, focused on digital strategy."),
                        mia("That's great. Why do you want to work here?"),
                        leo("I admire your company's focus on innovation and creativity."),
                        mia("What would you say is your biggest strength?"),
                        leo("I'm very good at solving problems under pressure."),
                        mia("Can you give an example of that?"),
                        leo("Sure, I once led a campaign that turned around a failing product launch."),
                        mia("Impressive. What about a weakness you're working on?"),
                        leo("I sometimes take on too much, but I've been learning to delegate."),
                        mia("Thank you, Leo. Do you have any questions for me?"),
                        leo("Yes, what does success look like in this role?")
                    ),
                    questions = listOf(
                        mcq("How many years of experience does Leo have?", listOf("Five", "Two", "Ten", "One"), 0),
                        mcq("What does Leo admire about the company?", listOf("Its focus on innovation", "Its salary", "Its location", "Its size"), 0),
                        fib("Leo says his strength is solving problems under ___.", listOf("pressure", "stress", "time", "rules"), 0),
                        mcq("What example does Leo give?", listOf("Turning around a failing product launch", "Hiring new staff", "Cutting costs", "Winning an award"), 0),
                        fib("Leo is working on learning to ___.", listOf("delegate", "relax", "code", "travel"), 0),
                        mcq("What does Leo ask at the end?", listOf("What does success look like in this role", "What is the salary", "When can he start", "Who is his manager"), 0)
                    )
                )
            ),
            Chapter(
                number = 2,
                title = "Around the Office",
                description = "Learn common workplace object names with picture flashcards.",
                exercise = pictureExercise(
                    cards = listOf(
                        PictureCard("advanced-interview-ch2-card1", "laptop", "laptop", "She opened her laptop to check her email."),
                        PictureCard("advanced-interview-ch2-card2", "nametag", "name tag", "Everyone at the conference wore a name tag."),
                        PictureCard("advanced-interview-ch2-card3", "whiteboard", "whiteboard", "He sketched the plan out on the whiteboard."),
                        PictureCard("advanced-interview-ch2-card4", "briefcase", "briefcase", "He carried his laptop in a black briefcase."),
                        PictureCard("advanced-interview-ch2-card5", "resume", "resume", "She updated her resume before the interview.")
                    )
                )
            ),
            Chapter(
                number = 3,
                title = "Asking Smart Questions",
                description = "Practice asking thoughtful questions at the end of an interview.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        leo("What does a typical day look like in this role?"),
                        mia("You'd start with a team stand-up, then work on client projects."),
                        leo("How would you describe the team culture?"),
                        mia("We're collaborative and value open communication."),
                        leo("What are the next steps in the process?"),
                        mia("We'll follow up within a week with our decision.")
                    ),
                    questions = listOf(
                        mcq("What starts a typical day?", listOf("A team stand-up", "A meeting with clients", "Lunch", "Training"), 0),
                        mcq("How is the team culture described?", listOf("Collaborative", "Competitive", "Strict", "Quiet"), 0),
                        fib("They will follow up within a ___.", listOf("week", "day", "month", "year"), 0)
                    )
                )
            )
        )
    )

    // MARK: Advanced extras

    val advancedNegotiating = Course(
        id = "advanced-negotiating",
        title = "Negotiating a Deal",
        description = "Learn how to discuss terms and find common ground professionally.",
        level = CourseLevel.ADVANCED,
        accent = CourseAccent.CORAL,
        highlights = listOf("Negotiating", "Compromise"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Finding Middle Ground",
                description = "Practice negotiating terms professionally.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("Your proposal is close, but the timeline is tight."),
                        leo("We could extend it by two weeks if that helps."),
                        mia("That works for us. Let's put it in writing."),
                        leo("Agreed, I'll send the revised contract today.")
                    ),
                    questions = listOf(
                        mcq("What was Mia's concern?", listOf("The timeline", "The price", "The location", "The team size"), 0),
                        fib("Leo offers to extend it by two ___.", listOf("weeks", "days", "months", "hours"), 0)
                    )
                )
            )
        )
    )

    val advancedPresentation = Course(
        id = "advanced-presentation",
        title = "Giving a Presentation",
        description = "Practice presenting ideas clearly and fielding follow-up questions.",
        level = CourseLevel.ADVANCED,
        accent = CourseAccent.CORAL,
        highlights = listOf("Presenting", "Q&A"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Handling Questions",
                description = "Learn how to field tough questions confidently after a talk.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        leo("That's a great question. Let me break it down."),
                        mia("Sure, go ahead."),
                        leo("The data shows a steady rise over three quarters."),
                        mia("That clears it up, thank you.")
                    ),
                    questions = listOf(
                        mcq("What does the data show?", listOf("A steady rise", "A steep decline", "No change", "Missing data"), 0),
                        fib("The rise happened over three ___.", listOf("quarters", "weeks", "days", "years"), 0)
                    )
                )
            )
        )
    )

    val advancedComplaints = Course(
        id = "advanced-complaints",
        title = "Handling Complaints",
        description = "Learn how to respond to complaints calmly and professionally.",
        level = CourseLevel.ADVANCED,
        accent = CourseAccent.CORAL,
        highlights = listOf("Complaints", "De-escalation"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Staying Professional",
                description = "Practice responding calmly and professionally to a complaint.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("I'm frustrated, my order arrived a week late."),
                        leo("I completely understand, and I'm sorry for the delay."),
                        mia("What can you do about it?"),
                        leo("I'll refund the shipping and expedite your next order.")
                    ),
                    questions = listOf(
                        mcq("What was Mia's complaint about?", listOf("A late order", "A wrong item", "A rude staff member", "A billing error"), 0),
                        fib("Leo offers to refund the ___.", listOf("shipping", "full order", "tax", "nothing"), 0)
                    )
                )
            )
        )
    )

    val advancedDebate = Course(
        id = "advanced-debate",
        title = "Debating an Issue",
        description = "Practice presenting a viewpoint and responding to a counterargument respectfully.",
        level = CourseLevel.ADVANCED,
        accent = CourseAccent.CORAL,
        highlights = listOf("Debate", "Persuasion"),
        chapters = listOf(
            Chapter(
                number = 1,
                title = "Making Your Case",
                description = "Practice presenting and defending a viewpoint respectfully.",
                exercise = dialogueExercise(
                    dialogue = listOf(
                        mia("I think remote work improves productivity."),
                        leo("I see your point, but I'd argue collaboration suffers."),
                        mia("That's fair. Maybe a hybrid model is the answer."),
                        leo("I could get behind that compromise.")
                    ),
                    questions = listOf(
                        mcq("What does Mia argue improves productivity?", listOf("Remote work", "Longer hours", "More meetings", "Bigger teams"), 0),
                        fib("They agree a ___ model might be the answer.", listOf("hybrid", "fully remote", "fully in-office", "four-day"), 0)
                    )
                )
            )
        )
    )
}