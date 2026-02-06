// Int primitive examples (spec section 2.1)
// Signed 64-bit integer, immutable and serializable

// Literals
let a = 42;
let b = -(17);
let c = 1_000_000;
let zero = 0;

// Arithmetic: +, -, *, /, %
let sum = a + 10;
let diff = a - 10;
let prod = a * 2;
let quot = 100 / 3;
let remainder = 7 % 3;

// Comparisons
let lt = 1 < 2;
let le = 2 <= 2;
let gt = 3 > 1;
let ge = 2 >= 2;
let eq = 5 == 5;
let ne = 5 != 3;

// Precedence
let prec1 = 2 + 3 * 4;       // 14
let prec2 = (2 + 3) * 4;     // 20

// In functions
fn factorial(n: Int): Int {
    if n < 2 { 1 } else { n * factorial(n - 1) }
}
let result = factorial(6);    // 720
