// Factorial showcasing v0.1 syntax

// Recursive version
fn factorial(n: Int): Int =
    if n < 2 { 1 } else { n * factorial(n - 1) };

// Iterative version
fn factorial_iter(n: Int): Int {
    let result = 1;
    for i in 2..=n {
        result = result * i;
    }
    result
}

let result = factorial(5);
