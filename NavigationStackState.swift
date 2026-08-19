import Foundation
import Combine

class NavigationStackState: ObservableObject {
    @Published private var stack: [String]
    
    init(initial: String) {
        self.stack = [initial]
    }
    
    var current: String {
        get { stack.last ?? "" }
        set {
            if stack.last != newValue {
                stack.append(newValue)
            }
        }
    }
    
    var canGoBack: Bool {
        stack.count > 1
    }
    
    func goBack() {
        if stack.count > 1 {
            stack.removeLast()
        }
    }
}
