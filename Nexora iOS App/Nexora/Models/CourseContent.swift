
import SwiftUI

// MARK: - Level

enum CourseLevel: String, CaseIterable, Identifiable, Hashable {
    case beginner = "Beginner"
    case intermediate = "Intermediate"
    case advanced = "Advanced"

    var id: String { rawValue }
}

/// Cycles the brand gradient across levels instead of picking colors
/// per-course, same pattern as VideoTopic.tint.
enum CourseAccent: Hashable {
    case indigo, teal, coral

    var tint: Color {
        switch self {
        case .indigo: return AppColors.gradientTop
        case .teal: return AppColors.gradientMid
        case .coral: return AppColors.gradientBottom
        }
    }
}

// MARK: - Course / Chapter

struct Course: Identifiable, Hashable {
    let id: String
    let title: String
    let description: String
    let level: CourseLevel
    let accent: CourseAccent
    let highlights: [String]
    let chapters: [Chapter]
}

struct Chapter: Identifiable, Hashable {
    let id = UUID()
    let number: Int
    let title: String
    let description: String
    let exercise: ChapterExerciseContent
}

// MARK: - Exercise (dialogue + quiz)

struct Exercise: Hashable {
    let title: String
    let instructions: String
    let dialogue: [DialogueLine]
    let questions: [QuizQuestion]
}

struct DialogueLine: Identifiable, Hashable {
    let id = UUID()
    let speaker: DialogueSpeaker
    let text: String
}

enum DialogueSpeaker: String, Hashable {
    case mia = "Mia"
    case leo = "Leo"
}

enum QuizQuestionType: Hashable {
    case multipleChoice
    case fillInBlank
}

struct QuizQuestion: Identifiable, Hashable {
    let id = UUID()
    let type: QuizQuestionType
    let prompt: String
    let options: [String]
    let correctOptionIndex: Int
}

// MARK: - Sample Catalog

enum CourseCatalog {

    static let courses: [Course] = [
        beginner,
        beginnerSmallTalk,
        beginnerDirections,
        beginnerWeather,
        beginnerFamily,
        intermediate,
        intermediatePlans,
        intermediatePhone,
        intermediateShopping,
        intermediateDoctor,
        advanced,
        advancedNegotiating,
        advancedPresentation,
        advancedComplaints,
        advancedDebate
    ]

    // MARK: - Beginner — Everyday Greetings

    static let beginner = Course(
        id: "beginner-greetings",
        title: "Everyday Greetings",
        description: "Learn how to greet people, introduce yourself, and keep a friendly conversation going.",
        level: .beginner,
        accent: .indigo,
        highlights: ["Greetings", "Small talk", "Introductions"],
        chapters: [
            Chapter(
                number: 1,
                title: "Saying Hello",
                description: "Learn how to greet people and start a simple conversation.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Hi Leo! How are you today?"),
                        DialogueLine(speaker: .leo, text: "I'm doing great, thanks! How about you?"),
                        DialogueLine(speaker: .mia, text: "I'm good too. Have you met our new neighbor?"),
                        DialogueLine(speaker: .leo, text: "Not yet. What's her name?"),
                        DialogueLine(speaker: .mia, text: "Her name is Sarah. She just moved in yesterday."),
                        DialogueLine(speaker: .leo, text: "Nice! I should go say hello to her."),
                        DialogueLine(speaker: .mia, text: "That's a great idea. She seems very friendly."),
                        DialogueLine(speaker: .leo, text: "Do you know where she's from?"),
                        DialogueLine(speaker: .mia, text: "I think she mentioned she's from Canada."),
                        DialogueLine(speaker: .leo, text: "Cool! I'll ask her more about it later."),
                        DialogueLine(speaker: .mia, text: "Sounds good. See you at lunch, Leo!"),
                        DialogueLine(speaker: .leo, text: "See you then, Mia. Have a great morning!")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "How is Leo doing today?", options: ["Terrible", "Great", "Tired", "Busy"], correctOptionIndex: 1),
                        QuizQuestion(type: .multipleChoice, prompt: "What is the new neighbor's name?", options: ["Sarah", "Mia", "Leo", "Anna"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Sarah just ___ in yesterday.", options: ["moved", "left", "called", "visited"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "Where is Sarah from?", options: ["Canada", "France", "Japan", "Mexico"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo will ask Sarah more about it ___.", options: ["later", "never", "yesterday", "now"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "When will Mia and Leo see each other again?", options: ["At lunch", "Tomorrow", "Never", "At dinner"], correctOptionIndex: 0)
                    ]
                ))
            ),

            Chapter(
                number: 2,
                title: "Around the House",
                description: "Learn everyday household object names with picture flashcards.",
                exercise: .picture(PictureExercise(
                    title: "Picture Vocabulary",
                    instructions: "Study the picture, type your guess, then reveal the answer.",
                    cards: [
                        PictureCard(id: "beginner-greetings-ch2-card1", imageName: "umbrella", answer: "umbrella", exampleSentence: "Take an umbrella, it might rain later."),
                        PictureCard(id: "beginner-greetings-ch2-card2", imageName: "clock", answer: "clock", exampleSentence: "The clock on the wall says three o'clock."),
                        PictureCard(id: "beginner-greetings-ch2-card3", imageName: "chair", answer: "chair", exampleSentence: "Please pull up a chair and sit down."),
                        PictureCard(id: "beginner-greetings-ch2-card4", imageName: "mug", answer: "mug", exampleSentence: "She poured coffee into her favorite mug."),
                        PictureCard(id: "beginner-greetings-ch2-card5", imageName: "lamp", answer: "lamp", exampleSentence: "Turn on the lamp, it's getting dark.")
                    ]
                ))
            ),

            Chapter(
                number: 3,
                title: "Making Small Talk",
                description: "Learn common small talk phrases about the weather and weekend plans.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Nice weather today, isn't it?"),
                        DialogueLine(speaker: .leo, text: "Yes, it's really sunny! Perfect for a walk."),
                        DialogueLine(speaker: .mia, text: "Do you have any plans for the weekend?"),
                        DialogueLine(speaker: .leo, text: "I'm going hiking with some friends. What about you?"),
                        DialogueLine(speaker: .mia, text: "I'm just going to relax at home."),
                        DialogueLine(speaker: .leo, text: "That sounds nice too. Enjoy your weekend, Mia!")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What is the weather like today?", options: ["Sunny", "Rainy", "Snowy", "Windy"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What is Leo doing this weekend?", options: ["Hiking", "Working", "Sleeping", "Studying"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Mia is going to ___ at home.", options: ["relax", "cook", "clean", "paint"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Beginner — Making Small Talk

    static let beginnerSmallTalk = Course(
        id: "beginner-small-talk",
        title: "Making Small Talk",
        description: "Pick up easy openers for chatting about everyday topics with someone new.",
        level: .beginner,
        accent: .indigo,
        highlights: ["Icebreakers", "New people"],
        chapters: [
            Chapter(
                number: 1,
                title: "Breaking the Ice",
                description: "Learn easy openers for chatting with someone new.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Hi, I don't think we've met before."),
                        DialogueLine(speaker: .leo, text: "No, I'm new here. I'm Leo."),
                        DialogueLine(speaker: .mia, text: "Welcome, Leo! How do you like it so far?"),
                        DialogueLine(speaker: .leo, text: "Everyone's been really friendly. I like it a lot.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "Is Leo new here?", options: ["Yes", "No", "Not sure", "He's leaving"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo says everyone has been really ___.", options: ["friendly", "busy", "quiet", "strict"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Beginner — Asking for Directions

    static let beginnerDirections = Course(
        id: "beginner-directions",
        title: "Asking for Directions",
        description: "Learn how to ask for and follow directions when you're finding your way.",
        level: .beginner,
        accent: .indigo,
        highlights: ["Directions", "Locations"],
        chapters: [
            Chapter(
                number: 1,
                title: "Finding Your Way",
                description: "Practice asking for and giving simple directions.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Excuse me, how do I get to the train station?"),
                        DialogueLine(speaker: .leo, text: "Go straight two blocks, then turn left."),
                        DialogueLine(speaker: .mia, text: "Straight, then left. Is it far?"),
                        DialogueLine(speaker: .leo, text: "No, it's only a five-minute walk.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "Where does Mia want to go?", options: ["Train station", "Bus stop", "Airport", "Hotel"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "It's only a ___-minute walk.", options: ["five", "twenty", "one", "sixty"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Beginner — Talking About the Weather

    static let beginnerWeather = Course(
        id: "beginner-weather",
        title: "Talking About the Weather",
        description: "Practice the go-to phrases for describing what it's like outside.",
        level: .beginner,
        accent: .indigo,
        highlights: ["Weather talk", "Descriptions"],
        chapters: [
            Chapter(
                number: 1,
                title: "Hot or Cold",
                description: "Learn phrases for describing today's weather.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "It's freezing out today!"),
                        DialogueLine(speaker: .leo, text: "I know, I should have worn a warmer jacket."),
                        DialogueLine(speaker: .mia, text: "They said it might snow later."),
                        DialogueLine(speaker: .leo, text: "Really? I hope it doesn't stick to the roads.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "How does Mia describe the weather?", options: ["Freezing", "Hot", "Rainy", "Windy"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "It might ___ later, they said.", options: ["snow", "clear up", "get hot", "flood"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Beginner — Talking About Family

    static let beginnerFamily = Course(
        id: "beginner-family",
        title: "Talking About Family",
        description: "Learn how to talk about your family members and ask about someone else's.",
        level: .beginner,
        accent: .indigo,
        highlights: ["Family", "Describing people"],
        chapters: [
            Chapter(
                number: 1,
                title: "Meet the Family",
                description: "Learn how to describe your family members.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Do you have any brothers or sisters, Leo?"),
                        DialogueLine(speaker: .leo, text: "I have one younger sister. What about you?"),
                        DialogueLine(speaker: .mia, text: "I have two older brothers."),
                        DialogueLine(speaker: .leo, text: "That sounds like a lively house growing up!")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "How many siblings does Leo have?", options: ["One", "Two", "Three", "None"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Mia has two ___ brothers.", options: ["older", "younger", "twin", "step"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Intermediate — At the Restaurant

    static let intermediate = Course(
        id: "intermediate-restaurant",
        title: "At the Restaurant",
        description: "Order food with confidence, ask about the menu, and handle the bill like a regular.",
        level: .intermediate,
        accent: .teal,
        highlights: ["Ordering", "Menu questions", "Paying"],
        chapters: [
            Chapter(
                number: 1,
                title: "Ordering Food",
                description: "Learn phrases for ordering food and drinks at a restaurant.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Good evening! Table for two, please."),
                        DialogueLine(speaker: .leo, text: "Of course, right this way. Here are your menus."),
                        DialogueLine(speaker: .mia, text: "Thank you. Could we have a few minutes to decide?"),
                        DialogueLine(speaker: .leo, text: "Take your time. I'll come back shortly."),
                        DialogueLine(speaker: .mia, text: "I think I'm ready to order now."),
                        DialogueLine(speaker: .leo, text: "Great, what would you like to have?"),
                        DialogueLine(speaker: .mia, text: "I'll have the grilled salmon with a side salad."),
                        DialogueLine(speaker: .leo, text: "Excellent choice. Anything to drink?"),
                        DialogueLine(speaker: .mia, text: "Just a glass of water, please."),
                        DialogueLine(speaker: .leo, text: "Sure thing. I'll put your order in right away."),
                        DialogueLine(speaker: .mia, text: "Thank you so much!"),
                        DialogueLine(speaker: .leo, text: "You're welcome. Your food will be out shortly.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "How many people are in Mia's party?", options: ["Two", "Three", "One", "Four"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "The waiter says, \"Take your ___.\"", options: ["time", "order", "seat", "menu"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What did Mia order to eat?", options: ["Grilled salmon", "Steak", "Pasta", "Pizza"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What did Mia order to drink?", options: ["Water", "Soda", "Juice", "Coffee"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "The waiter will put the order in ___ away.", options: ["right", "some", "far", "never"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What does Mia say at the end?", options: ["Thank you so much", "Goodbye", "See you later", "No thanks"], correctOptionIndex: 0)
                    ]
                ))
            ),

            Chapter(
                number: 2,
                title: "On the Menu",
                description: "Learn common restaurant menu items and table settings with picture flashcards.",
                exercise: .picture(PictureExercise(
                    title: "Picture Vocabulary",
                    instructions: "Study the picture, type your guess, then reveal the answer.",
                    cards: [
                        PictureCard(id: "intermediate-restaurant-ch2-card1", imageName: "menu", answer: "menu", exampleSentence: "The waiter handed us each a menu."),
                        PictureCard(id: "intermediate-restaurant-ch2-card2", imageName: "fork", answer: "fork", exampleSentence: "She picked up her fork and started eating."),
                        PictureCard(id: "intermediate-restaurant-ch2-card3", imageName: "wine-glass", answer: "wine glass", exampleSentence: "He filled the wine glass halfway."),
                        PictureCard(id: "intermediate-restaurant-ch2-card4", imageName: "napkin", answer: "napkin", exampleSentence: "She wiped her hands on a napkin."),
                        PictureCard(id: "intermediate-restaurant-ch2-card5", imageName: "dessert", answer: "dessert", exampleSentence: "We shared a slice of chocolate dessert.")
                    ]
                ))
            ),

            Chapter(
                number: 3,
                title: "Paying the Bill",
                description: "Learn how to ask for the bill and split payment.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Could we have the bill, please?"),
                        DialogueLine(speaker: .leo, text: "Of course, I'll bring it right over."),
                        DialogueLine(speaker: .mia, text: "Can we split it between two cards?"),
                        DialogueLine(speaker: .leo, text: "Absolutely, that's no problem at all."),
                        DialogueLine(speaker: .mia, text: "Great, thank you for a lovely dinner."),
                        DialogueLine(speaker: .leo, text: "You're very welcome. Have a great night!")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What does Mia ask for?", options: ["The bill", "More food", "A refund", "A table"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "How does Mia want to pay?", options: ["Split between two cards", "Cash only", "One card", "Gift card"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo says, \"Have a great ___!\"", options: ["night", "day", "trip", "meal"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Intermediate — Making Plans

    static let intermediatePlans = Course(
        id: "intermediate-plans",
        title: "Making Plans",
        description: "Learn how to suggest, confirm, and adjust plans with a friend.",
        level: .intermediate,
        accent: .teal,
        highlights: ["Making plans", "Scheduling"],
        chapters: [
            Chapter(
                number: 1,
                title: "Setting a Time",
                description: "Practice suggesting and confirming plans with a friend.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Are you free this Saturday?"),
                        DialogueLine(speaker: .leo, text: "I think so. What did you have in mind?"),
                        DialogueLine(speaker: .mia, text: "Maybe we could catch a movie in the afternoon."),
                        DialogueLine(speaker: .leo, text: "Sounds great, let's say 3 PM?")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What day are they planning for?", options: ["Saturday", "Sunday", "Friday", "Monday"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "They agree to meet at ___ PM.", options: ["3", "5", "8", "12"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Intermediate — Talking on the Phone

    static let intermediatePhone = Course(
        id: "intermediate-phone",
        title: "Talking on the Phone",
        description: "Practice everyday phone etiquette, from answering to taking a message.",
        level: .intermediate,
        accent: .teal,
        highlights: ["Phone calls", "Messages"],
        chapters: [
            Chapter(
                number: 1,
                title: "Taking a Message",
                description: "Learn phrases for phone calls and leaving messages.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Hello, is Leo available?"),
                        DialogueLine(speaker: .leo, text: "This is Leo speaking."),
                        DialogueLine(speaker: .mia, text: "Great, I'm calling about tomorrow's meeting."),
                        DialogueLine(speaker: .leo, text: "Sure, let me grab a pen to take notes.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "Who answers the phone?", options: ["Leo", "Mia", "A receptionist", "No one"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Mia is calling about tomorrow's ___.", options: ["meeting", "party", "flight", "lunch"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Intermediate — Shopping for Clothes

    static let intermediateShopping = Course(
        id: "intermediate-shopping",
        title: "Shopping for Clothes",
        description: "Learn how to ask about sizes, colors, and where to try things on.",
        level: .intermediate,
        accent: .teal,
        highlights: ["Shopping", "Sizes & fit"],
        chapters: [
            Chapter(
                number: 1,
                title: "Finding the Right Size",
                description: "Practice asking about sizes, colors, and trying things on.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Do you have this jacket in a medium?"),
                        DialogueLine(speaker: .leo, text: "Let me check. Yes, here you go."),
                        DialogueLine(speaker: .mia, text: "Perfect, is there a fitting room nearby?"),
                        DialogueLine(speaker: .leo, text: "Right this way, second door on the left.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What size does Mia ask for?", options: ["Medium", "Small", "Large", "Extra large"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "The fitting room is the second door on the ___.", options: ["left", "right", "back", "front"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Intermediate — At the Doctor's Office

    static let intermediateDoctor = Course(
        id: "intermediate-doctor",
        title: "At the Doctor's Office",
        description: "Learn how to describe symptoms and understand a doctor's questions.",
        level: .intermediate,
        accent: .teal,
        highlights: ["Symptoms", "Health"],
        chapters: [
            Chapter(
                number: 1,
                title: "Describing Symptoms",
                description: "Learn how to describe how you're feeling to a doctor.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "What seems to be the problem today?"),
                        DialogueLine(speaker: .leo, text: "I've had a sore throat since Monday."),
                        DialogueLine(speaker: .mia, text: "Any fever or trouble swallowing?"),
                        DialogueLine(speaker: .leo, text: "A mild fever, but no trouble swallowing.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What is Leo's main symptom?", options: ["Sore throat", "Headache", "Back pain", "Cough"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo has had it since ___.", options: ["Monday", "Friday", "yesterday", "last month"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Advanced — Job Interview Skills

    static let advanced = Course(
        id: "advanced-interview",
        title: "Job Interview Skills",
        description: "Prepare to answer common interview questions clearly and confidently.",
        level: .advanced,
        accent: .coral,
        highlights: ["Interviews", "Strengths", "Follow-up Qs"],
        chapters: [
            Chapter(
                number: 1,
                title: "Answering Common Questions",
                description: "Practice answering typical interview questions with confidence.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Thanks for coming in today, Leo. Tell me about yourself."),
                        DialogueLine(speaker: .leo, text: "Sure. I have five years of experience in marketing, focused on digital strategy."),
                        DialogueLine(speaker: .mia, text: "That's great. Why do you want to work here?"),
                        DialogueLine(speaker: .leo, text: "I admire your company's focus on innovation and creativity."),
                        DialogueLine(speaker: .mia, text: "What would you say is your biggest strength?"),
                        DialogueLine(speaker: .leo, text: "I'm very good at solving problems under pressure."),
                        DialogueLine(speaker: .mia, text: "Can you give an example of that?"),
                        DialogueLine(speaker: .leo, text: "Sure, I once led a campaign that turned around a failing product launch."),
                        DialogueLine(speaker: .mia, text: "Impressive. What about a weakness you're working on?"),
                        DialogueLine(speaker: .leo, text: "I sometimes take on too much, but I've been learning to delegate."),
                        DialogueLine(speaker: .mia, text: "Thank you, Leo. Do you have any questions for me?"),
                        DialogueLine(speaker: .leo, text: "Yes, what does success look like in this role?")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "How many years of experience does Leo have?", options: ["Five", "Two", "Ten", "One"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What does Leo admire about the company?", options: ["Its focus on innovation", "Its salary", "Its location", "Its size"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo says his strength is solving problems under ___.", options: ["pressure", "stress", "time", "rules"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What example does Leo give?", options: ["Turning around a failing product launch", "Hiring new staff", "Cutting costs", "Winning an award"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo is working on learning to ___.", options: ["delegate", "relax", "code", "travel"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "What does Leo ask at the end?", options: ["What does success look like in this role", "What is the salary", "When can he start", "Who is his manager"], correctOptionIndex: 0)
                    ]
                ))
            ),

            Chapter(
                number: 2,
                title: "Around the Office",
                description: "Learn common workplace object names with picture flashcards.",
                exercise: .picture(PictureExercise(
                    title: "Picture Vocabulary",
                    instructions: "Study the picture, type your guess, then reveal the answer.",
                    cards: [
                        PictureCard(id: "advanced-interview-ch2-card1", imageName: "laptop", answer: "laptop", exampleSentence: "She opened her laptop to check her email."),
                        PictureCard(id: "advanced-interview-ch2-card2", imageName: "nametag", answer: "name tag", exampleSentence: "Everyone at the conference wore a name tag."),
                        PictureCard(id: "advanced-interview-ch2-card3", imageName: "whiteboard", answer: "whiteboard", exampleSentence: "He sketched the plan out on the whiteboard."),
                        PictureCard(id: "advanced-interview-ch2-card4", imageName: "briefcase", answer: "briefcase", exampleSentence: "He carried his laptop in a black briefcase."),
                        PictureCard(id: "advanced-interview-ch2-card5", imageName: "resume", answer: "resume", exampleSentence: "She updated her resume before the interview.")
                    ]
                ))
            ),

            Chapter(
                number: 3,
                title: "Asking Smart Questions",
                description: "Practice asking thoughtful questions at the end of an interview.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .leo, text: "What does a typical day look like in this role?"),
                        DialogueLine(speaker: .mia, text: "You'd start with a team stand-up, then work on client projects."),
                        DialogueLine(speaker: .leo, text: "How would you describe the team culture?"),
                        DialogueLine(speaker: .mia, text: "We're collaborative and value open communication."),
                        DialogueLine(speaker: .leo, text: "What are the next steps in the process?"),
                        DialogueLine(speaker: .mia, text: "We'll follow up within a week with our decision.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What starts a typical day?", options: ["A team stand-up", "A meeting with clients", "Lunch", "Training"], correctOptionIndex: 0),
                        QuizQuestion(type: .multipleChoice, prompt: "How is the team culture described?", options: ["Collaborative", "Competitive", "Strict", "Quiet"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "They will follow up within a ___.", options: ["week", "day", "month", "year"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Advanced — Negotiating a Deal

    static let advancedNegotiating = Course(
        id: "advanced-negotiating",
        title: "Negotiating a Deal",
        description: "Learn how to discuss terms and find common ground professionally.",
        level: .advanced,
        accent: .coral,
        highlights: ["Negotiating", "Compromise"],
        chapters: [
            Chapter(
                number: 1,
                title: "Finding Middle Ground",
                description: "Practice negotiating terms professionally.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "Your proposal is close, but the timeline is tight."),
                        DialogueLine(speaker: .leo, text: "We could extend it by two weeks if that helps."),
                        DialogueLine(speaker: .mia, text: "That works for us. Let's put it in writing."),
                        DialogueLine(speaker: .leo, text: "Agreed, I'll send the revised contract today.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What was Mia's concern?", options: ["The timeline", "The price", "The location", "The team size"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo offers to extend it by two ___.", options: ["weeks", "days", "months", "hours"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Advanced — Giving a Presentation

    static let advancedPresentation = Course(
        id: "advanced-presentation",
        title: "Giving a Presentation",
        description: "Practice presenting ideas clearly and fielding follow-up questions.",
        level: .advanced,
        accent: .coral,
        highlights: ["Presenting", "Q&A"],
        chapters: [
            Chapter(
                number: 1,
                title: "Handling Questions",
                description: "Learn how to field tough questions confidently after a talk.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .leo, text: "That's a great question. Let me break it down."),
                        DialogueLine(speaker: .mia, text: "Sure, go ahead."),
                        DialogueLine(speaker: .leo, text: "The data shows a steady rise over three quarters."),
                        DialogueLine(speaker: .mia, text: "That clears it up, thank you.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What does the data show?", options: ["A steady rise", "A steep decline", "No change", "Missing data"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "The rise happened over three ___.", options: ["quarters", "weeks", "days", "years"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Advanced — Handling Complaints

    static let advancedComplaints = Course(
        id: "advanced-complaints",
        title: "Handling Complaints",
        description: "Learn how to respond to complaints calmly and professionally.",
        level: .advanced,
        accent: .coral,
        highlights: ["Complaints", "De-escalation"],
        chapters: [
            Chapter(
                number: 1,
                title: "Staying Professional",
                description: "Practice responding calmly and professionally to a complaint.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "I'm frustrated, my order arrived a week late."),
                        DialogueLine(speaker: .leo, text: "I completely understand, and I'm sorry for the delay."),
                        DialogueLine(speaker: .mia, text: "What can you do about it?"),
                        DialogueLine(speaker: .leo, text: "I'll refund the shipping and expedite your next order.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What was Mia's complaint about?", options: ["A late order", "A wrong item", "A rude staff member", "A billing error"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "Leo offers to refund the ___.", options: ["shipping", "full order", "tax", "nothing"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )

    // MARK: - Advanced — Debating an Issue

    static let advancedDebate = Course(
        id: "advanced-debate",
        title: "Debating an Issue",
        description: "Practice presenting a viewpoint and responding to a counterargument respectfully.",
        level: .advanced,
        accent: .coral,
        highlights: ["Debate", "Persuasion"],
        chapters: [
            Chapter(
                number: 1,
                title: "Making Your Case",
                description: "Practice presenting and defending a viewpoint respectfully.",
                exercise: .dialogue(Exercise(
                    title: "Dialogue Exercise",
                    instructions: "Read the dialogue below, then answer questions about it from memory.",
                    dialogue: [
                        DialogueLine(speaker: .mia, text: "I think remote work improves productivity."),
                        DialogueLine(speaker: .leo, text: "I see your point, but I'd argue collaboration suffers."),
                        DialogueLine(speaker: .mia, text: "That's fair. Maybe a hybrid model is the answer."),
                        DialogueLine(speaker: .leo, text: "I could get behind that compromise.")
                    ],
                    questions: [
                        QuizQuestion(type: .multipleChoice, prompt: "What does Mia argue improves productivity?", options: ["Remote work", "Longer hours", "More meetings", "Bigger teams"], correctOptionIndex: 0),
                        QuizQuestion(type: .fillInBlank, prompt: "They agree a ___ model might be the answer.", options: ["hybrid", "fully remote", "fully in-office", "four-day"], correctOptionIndex: 0)
                    ]
                ))
            )
        ]
    )
}
