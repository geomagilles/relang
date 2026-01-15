fn calculate(a, b) {
    sum = a + b;
    diff = a - b;
    prod = a * b;
    quot = a / b;
    return sum + diff + prod + quot;
}

fn max(a, b) {
    if (a < b) {
        return b;
    }
    return a;
}

fn equals(a, b) {
    if (a == b) {
        return 1;
    }
    return 0;
}

r1 = calculate(10, 2);
r2 = max(42, 17);
r3 = equals(5, 5);
