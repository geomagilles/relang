// Bool primitive examples (spec section 3.1)

// Literals
let ok = true;
let ko = false;

// Logical operators
let a1 = true and true;      // true
let a2 = true and false;     // false
let o1 = false or true;      // true
let n1 = not true;           // false

// Complex
let complex = (true or false) and not false;  // true

// From comparisons
let fromCmp = 1 < 2;         // true

// In conditions
let r = if ok { "yes" } else { "no" };

// In functions
fn isPositive(n: Int): Bool { n > 0 }
let pos = isPositive(5);     // true
