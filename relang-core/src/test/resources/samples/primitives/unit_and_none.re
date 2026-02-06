// Unit and None examples (spec sections 6.1, 6.2)

// Unit: "no meaningful business value"
let u = unit;

// None: absence of value (optionals)
let absent = none;

// None in match
let x = none;
let r = match x {
    none -> "absent",
    _ -> "present"
};

// Function that may return none
fn findOrNone(x: Int) {
    if x > 0 { x } else { none }
}

let found = findOrNone(5);
let notFound = findOrNone(-(1));

let check = match notFound {
    none -> "not found",
    _ -> "found"
};
