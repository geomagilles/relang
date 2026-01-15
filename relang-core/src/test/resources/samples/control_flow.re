fn abs(x) {
    if (x < 0) {
        return 0 - x;
    } else {
        return x;
    }
}

fn is_even(n) {
    half = n / 2;
    doubled = half * 2;
    if (doubled == n) {
        return 1;
    }
    return 0;
}

fn count_down(n) {
    while (n < 1 == 0) {
        n = n - 1;
    }
    return n;
}

r1 = abs(0 - 42);
r2 = is_even(10);
r3 = count_down(5);
