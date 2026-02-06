// Return semantics (spec sections 3.1, 3.2)

// Implicit return: last expression
fn process(x: Int): Int {
    let y = x * 2;
    let z = y + 1;
    z
}

// Implicit return: if-expression
fn max(a: Int, b: Int): Int {
    if a > b { a } else { b }
}

// Implicit return: match-expression
fn describe(n: Int): String {
    match n {
        0 -> "zero",
        1 -> "one",
        _ -> "many"
    }
}

// Early return
fn abs(x: Int): Int {
    if x < 0 { return -(x); }
    x
}

// Early return in loop
fn findFirst(target: Int): Int {
    for i in 0..100 {
        if i == target { return i; }
    }
    -(1)
}

let r1 = process(10);       // 21
let r2 = max(10, 20);       // 20
let r3 = describe(1);       // "one"
let r4 = abs(-(5));          // 5
let r5 = findFirst(42);     // 42
