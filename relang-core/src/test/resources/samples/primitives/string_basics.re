// String primitive examples (spec section 3.2)

// Literals
let s = "hello";
let empty = "";
let msg = "hello world";

// Escape sequences
let nl = "line1\nline2";
let tab = "col1\tcol2";

// Concatenation
let greeting = "hello" + " " + "world";

// Equality
let same = "abc" == "abc";      // true
let diff = "abc" != "def";      // true

// In functions
fn greet(name: String): String {
    return "Hello " + name;
}
let result = greet("World");    // "Hello World"
