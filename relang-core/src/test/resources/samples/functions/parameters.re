// Function parameters (spec section 2.3)

// Default values
fn greet(name: String, greeting: String = "Hello"): String {
    return greeting + " " + name;
}

let r1 = greet("Alice");          // "Hello Alice"
let r2 = greet("Alice", "Hi");    // "Hi Alice"

// Named arguments
fn sub(a: Int, b: Int): Int { return a - b; }

let r3 = sub(a: 10, b: 3);       // 7
let r4 = sub(b: 3, a: 10);       // 7 (order doesn't matter)

// Mixed positional and named
fn compute(a: Int, b: Int, c: Int): Int {
    return a + b + c;
}

let r5 = compute(1, c: 3, b: 2); // 6

// Multiple defaults
fn make(a: Int, b: Int = 10, c: Int = 100): Int {
    return a + b + c;
}

let r6 = make(1);                 // 111
let r7 = make(1, 20);             // 121
let r8 = make(1, 2, 3);           // 6
