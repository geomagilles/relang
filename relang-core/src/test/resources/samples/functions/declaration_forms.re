// Function declaration forms (spec section 2.1, 2.2)

// Block body with explicit return type
fn double(x: Int): Int {
    x * 2
}

// Expression body (preferred for one-liners)
fn triple(x: Int): Int = x * 3;

// Inferred return type
fn square(x: Int) = x * x;

// No parameters
fn version(): String = "1.0.0";

// Multiple parameters
fn add(a: Int, b: Int): Int = a + b;

// No meaningful return (Unit)
fn doNothing(): Unit {
    unit
}

// Multiple statements in block
fn process(x: Int): Int {
    let y = x * 2;
    let z = y + 1;
    z
}

let r1 = double(5);     // 10
let r2 = triple(5);     // 15
let r3 = square(5);     // 25
let r4 = version();     // "1.0.0"
let r5 = add(3, 4);     // 7
let r6 = process(10);   // 21
