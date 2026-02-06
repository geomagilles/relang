// Control flow showcasing v0.1 syntax
fn abs(x: Int): Int =
    if x < 0 { -x } else { x };

fn is_even(n: Int): Bool = n % 2 == 0;

fn count_down(n: Int): Int {
    let i = n;
    while i > 0 {
        i = i - 1;
    }
    i
}

fn sum_range(n: Int): Int {
    let total = 0;
    for i in 1..=n {
        total = total + i;
    }
    total
}

let r1 = abs(-42);
let r2 = is_even(10);
let r3 = count_down(5);
let r4 = sum_range(10);
