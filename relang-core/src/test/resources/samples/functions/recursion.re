// Recursion and hoisting (spec sections 3.3, 3.4)

// Direct recursion
fn factorial(n: Int): Int {
    if n <= 1 { 1 } else { n * factorial(n - 1) }
}

fn fib(n: Int): Int {
    if n < 2 { n } else { fib(n - 1) + fib(n - 2) }
}

// Mutual recursion (hoisting allows forward references)
fn isEven(n: Int): Bool {
    if n == 0 { true } else { isOdd(n - 1) }
}

fn isOdd(n: Int): Bool {
    if n == 0 { false } else { isEven(n - 1) }
}

// GCD with early return
fn gcd(a: Int, b: Int): Int {
    if b == 0 { return a; }
    gcd(b, a % b)
}

let r1 = factorial(6);      // 720
let r2 = fib(10);           // 55
let r3 = isEven(10);        // true
let r4 = isOdd(7);          // true
let r5 = gcd(48, 18);       // 6
