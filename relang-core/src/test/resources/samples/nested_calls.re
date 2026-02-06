// Nested function calls showcasing v0.1 syntax
fn double(x: Int): Int = x * 2;

fn triple(x: Int): Int = x * 3;

fn add(a: Int, b: Int): Int = a + b;

fn complex_calculation(n: Int): Int {
    let a = double(n);
    let b = triple(n);
    let c = add(a, b);
    double(c)
}

let result = complex_calculation(5);
