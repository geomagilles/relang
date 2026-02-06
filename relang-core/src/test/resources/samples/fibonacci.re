// Fibonacci showcasing v0.1 syntax

// Recursive version
fn fib(n: Int): Int =
    if n < 2 { n } else { fib(n - 1) + fib(n - 2) };

// Iterative version
fn fibonacci(n: Int): Int {
    if n < 2 { return n; }
    let a = 0;
    let b = 1;
    for i in 2..=n {
        let temp = a + b;
        a = b;
        b = temp;
    }
    b
}

let result = fibonacci(10);
