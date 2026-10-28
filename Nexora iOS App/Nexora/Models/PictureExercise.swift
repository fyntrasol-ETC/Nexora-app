
import Foundation

struct PictureCard: Identifiable, Hashable {
    let id: String

    let imageName: String
   
    let answer: String
    let exampleSentence: String
}

struct PictureExercise: Hashable {
    let title: String
    let instructions: String
    let cards: [PictureCard]
}


enum ChapterExerciseContent: Hashable {
    case dialogue(Exercise)
    case picture(PictureExercise)

    var title: String {
        switch self {
        case .dialogue(let e): return e.title
        case .picture(let e): return e.title
        }
    }

    var instructions: String {
        switch self {
        case .dialogue(let e): return e.instructions
        case .picture(let e): return e.instructions
        }
    }
}
