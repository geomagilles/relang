// Type annotation examples (spec section 2)
// Type annotations are parsed on function parameters and return types

// Fully annotated function
fn add(a: Int, b: Int): Int {
    return a + b;
}

// Optional return type
fn findOrNone(x: Int): Int? {
    if x > 0 { x } else { none }
}

// Expression body with annotations
fn double(x: Int): Int = x * 2;

// Without annotations (also valid)
fn square(x) = x * x;

// Mixed: some params annotated, some not
fn mixed(a: Int, b) { return a + b; }

let r1 = add(3, 4);          // 7
let r2 = findOrNone(5);      // 5
let r3 = double(21);         // 42
let r4 = square(6);          // 36
