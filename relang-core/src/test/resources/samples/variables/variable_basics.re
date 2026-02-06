// Variable declaration and reassignment examples (spec sections 2-3)

// Simple declarations
let x = 10;
let name = "Alice";
let ok = true;
let pi = 3.14;

// Declaration with expression
let sum = x + 5;

// Reassignment
let count = 0;
count = count + 1;
count = count + 1;

// Dependent declarations
let a = 10;
let b = a + 5;
let c = a + b;

// Optional (none) — none is its own type
let email = none;

// Swap
let p = 1;
let q = 2;
let tmp = p;
p = q;
q = tmp;
