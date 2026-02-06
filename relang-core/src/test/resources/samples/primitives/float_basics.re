// Float primitive examples (spec section 2.2)
// IEEE 754 64-bit double. Implicit widening Int -> Float in mixed operations.

// Literals
let x = 3.14;
let y = -(0.5);
let z = 1.0e6;
let w = 1_000.5;

// Arithmetic
let sum = 1.0 + 2.5;         // 3.5
let diff = 10.0 - 3.5;       // 6.5
let prod = 2.0 * 3.0;        // 6.0
let quot = 7.0 / 2.0;        // 3.5

// Comparisons
let lt = 1.0 < 2.0;
let eq = 3.14 == 3.14;
let ne = 3.14 != 2.0;

// In functions
fn half(x: Float): Float = x / 2.0;
let h = half(10.0);          // 5.0
