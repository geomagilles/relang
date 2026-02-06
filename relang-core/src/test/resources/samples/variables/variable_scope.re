// Variable scope examples (spec sections 4, 8)

// Function scope: each function has its own locals
fn getVal(): Int {
    let x = 99;
    x
}

let x = 1;
let r = getVal();
// x is still 1, r is 99

// Variables in blocks
let sum = 0;
for i in 0..5 {
    sum = sum + i;
}
// sum is 10

// Recursive: each call frame has independent variables
fn fib(n: Int): Int {
    if n < 2 { n } else { fib(n - 1) + fib(n - 2) }
}
let f = fib(10);  // 55
