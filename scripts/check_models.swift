import Foundation

@main
struct CheckModels {
    static func main() throws {
        let path = CommandLine.arguments[1]
        let dictionary = try JSONDecoder().decode(TeXDictionary.self, from: Data(contentsOf: URL(fileURLWithPath: path)))
        try dictionary.validate()
        precondition(dictionary.search(" ALPHA\n").contains(where: { $0.command == "\\alpha" }))
        precondition(dictionary.search("α").contains(where: { $0.command == "\\alpha" }))
        precondition(dictionary.search("alpha", category: "Formatting").isEmpty)
        precondition(!dictionary.search("amsmath").isEmpty)
        print("Swift model: bundled dictionary validation and user searches passed.")
    }
}
