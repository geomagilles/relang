fn double(x) {
    return x * 2;
}

fn triple(x) {
    return x * 3;
}

fn add(a, b) {
    return a + b;
}

fn complex_calculation(n) {
    a = double(n);
    b = triple(n);
    c = add(a, b);
    return double(c)
    }}

result = complex_calculation(5);
