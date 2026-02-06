// Arithmetic operations showcasing v0.1 syntax
fn calculate(a: Int, b: Int): Int {
    let sum = a + b;
    let diff = a - b;
    let prod = a * b;
    let quot = a / b;
    sum + diff + prod + quot
}

fn max(a: Int, b: Int): Int =
    if a < b { b } else { a };

fn equals(a: Int, b: Int): Bool = a == b;

let r1 = calculate(10, 2);
let r2 = max(42, 17);
let r3 = equals(5, 5);
