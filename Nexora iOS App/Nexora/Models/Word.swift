
import Foundation

struct Word: Identifiable, Hashable, Codable {
    enum Status: String, Codable, Hashable {
        case unknown   // not yet studied — every word starts here
        case know
        case dontKnow
    }

    enum Difficulty: String, Codable, CaseIterable, Hashable {
        case beginner, intermediate, advanced

        var displayName: String {
            switch self {
            case .beginner: return "Beginner"
            case .intermediate: return "Intermediate"
            case .advanced: return "Advanced"
            }
        }
    }

    let id: String
    var text: String
    var partOfSpeech: String
    var definition: String
    var example: String = ""
    var difficulty: Difficulty = .beginner
    var status: Status = .unknown
    var isSaved: Bool = false
}

// Seed data. NOT gated behind #if DEBUG — WordsViewModel's default
// initializer relies on this as real starter content, so hiding it from
// Release builds would break production.
extension Word {
    static let sampleWords: [Word] = beginnerWords + intermediateWords + advancedWords

    static let beginnerWords: [Word] = [
        Word(id: "b1", text: "happy", partOfSpeech: "adjective",
             definition: "Feeling or showing pleasure or contentment.",
             example: "She felt happy when she saw her friends.",
             difficulty: .beginner),
        Word(id: "b2", text: "quick", partOfSpeech: "adjective",
             definition: "Moving fast or done in a short time.",
             example: "He gave a quick answer to the question.",
             difficulty: .beginner),
        Word(id: "b3", text: "friendly", partOfSpeech: "adjective",
             definition: "Kind and pleasant to be around.",
             example: "Our new neighbor is very friendly.",
             difficulty: .beginner),
        Word(id: "b4", text: "explain", partOfSpeech: "verb",
             definition: "To make something clear by describing it.",
             example: "Can you explain how this works?",
             difficulty: .beginner),
        Word(id: "b5", text: "borrow", partOfSpeech: "verb",
             definition: "To take something with the intention of returning it.",
             example: "I borrowed a pen from my classmate.",
             difficulty: .beginner),
        Word(id: "b6", text: "weather", partOfSpeech: "noun",
             definition: "The state of the atmosphere at a place and time.",
             example: "The weather was sunny all week.",
             difficulty: .beginner),
        Word(id: "b7", text: "simple", partOfSpeech: "adjective",
             definition: "Easy to understand or do.",
             example: "The instructions were simple to follow.",
             difficulty: .beginner),
        Word(id: "b8", text: "arrive", partOfSpeech: "verb",
             definition: "To reach a place, especially at the end of a journey.",
             example: "We arrived at the airport early.",
             difficulty: .beginner),
        Word(id: "b9", text: "neighbor", partOfSpeech: "noun",
             definition: "A person who lives near another.",
             example: "My neighbor waters my plants when I travel.",
             difficulty: .beginner),
        Word(id: "b10", text: "paperwork", partOfSpeech: "noun",
             definition: "Documents that record or authorize something.",
             example: "I still have a stack of paperwork to finish before Friday.",
             difficulty: .beginner),
    ]

    static let intermediateWords: [Word] = [
        Word(id: "i1", text: "find one's way around", partOfSpeech: "verb",
             definition: "To learn where to go without getting lost.",
             example: "After a few weeks, I was able to find my way around the new office.",
             difficulty: .intermediate),
        Word(id: "i2", text: "fit in", partOfSpeech: "verb",
             definition: "To feel comfortable and accepted in a group or situation.",
             example: "It took a while, but she started to fit in with the team.",
             difficulty: .intermediate),
        Word(id: "i3", text: "persuade", partOfSpeech: "verb",
             definition: "To convince someone to do or believe something.",
             example: "He tried to persuade his boss to approve the budget.",
             difficulty: .intermediate),
        Word(id: "i4", text: "reliable", partOfSpeech: "adjective",
             definition: "Consistently good in quality; able to be trusted.",
             example: "She's a reliable colleague who always meets deadlines.",
             difficulty: .intermediate),
        Word(id: "i5", text: "postpone", partOfSpeech: "verb",
             definition: "To delay an event to a later time.",
             example: "We had to postpone the meeting due to bad weather.",
             difficulty: .intermediate),
        Word(id: "i6", text: "negotiate", partOfSpeech: "verb",
             definition: "To discuss something to reach an agreement.",
             example: "They negotiated a better price for the car.",
             difficulty: .intermediate),
        Word(id: "i7", text: "thorough", partOfSpeech: "adjective",
             definition: "Complete, with attention to every detail.",
             example: "The inspector did a thorough review of the building.",
             difficulty: .intermediate),
        Word(id: "i8", text: "cope with", partOfSpeech: "verb",
             definition: "To deal successfully with a difficult situation.",
             example: "It's hard to cope with so much stress at once.",
             difficulty: .intermediate),
        Word(id: "i9", text: "essential", partOfSpeech: "adjective",
             definition: "Absolutely necessary.",
             example: "Sleep is essential for good health.",
             difficulty: .intermediate),
        Word(id: "i10", text: "accomplish", partOfSpeech: "verb",
             definition: "To achieve or complete something successfully.",
             example: "She accomplished all her goals for the year.",
             difficulty: .intermediate),
    ]

    static let advancedWords: [Word] = [
        Word(id: "a1", text: "ambiguous", partOfSpeech: "adjective",
             definition: "Open to more than one interpretation.",
             example: "The contract's wording was ambiguous and caused confusion.",
             difficulty: .advanced),
        Word(id: "a2", text: "meticulous", partOfSpeech: "adjective",
             definition: "Showing great attention to detail.",
             example: "He's meticulous about checking his code before submitting it.",
             difficulty: .advanced),
        Word(id: "a3", text: "resilience", partOfSpeech: "noun",
             definition: "The capacity to recover quickly from difficulties.",
             example: "Her resilience helped her bounce back after the setback.",
             difficulty: .advanced),
        Word(id: "a4", text: "procrastinate", partOfSpeech: "verb",
             definition: "To delay doing something that should be done.",
             example: "He tends to procrastinate until the deadline is near.",
             difficulty: .advanced),
        Word(id: "a5", text: "ubiquitous", partOfSpeech: "adjective",
             definition: "Present or found everywhere.",
             example: "Smartphones have become ubiquitous in modern life.",
             difficulty: .advanced),
        Word(id: "a6", text: "mitigate", partOfSpeech: "verb",
             definition: "To make something less severe or harmful.",
             example: "The company took steps to mitigate the risk of data loss.",
             difficulty: .advanced),
        Word(id: "a7", text: "skeptical", partOfSpeech: "adjective",
             definition: "Having doubts about something.",
             example: "She remained skeptical of the salesman's claims.",
             difficulty: .advanced),
        Word(id: "a8", text: "inevitable", partOfSpeech: "adjective",
             definition: "Certain to happen; unavoidable.",
             example: "Change is inevitable in any growing company.",
             difficulty: .advanced),
        Word(id: "a9", text: "discrepancy", partOfSpeech: "noun",
             definition: "A difference between things that should match.",
             example: "There's a discrepancy between the two reports.",
             difficulty: .advanced),
        Word(id: "a10", text: "articulate", partOfSpeech: "adjective",
             definition: "Able to express ideas clearly and effectively.",
             example: "She gave an articulate presentation to the board.",
             difficulty: .advanced),
    ]
}
