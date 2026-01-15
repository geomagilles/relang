fn fibonacci(n) {
    if (n < 2) {
        return n;
    }

    a = 0;
    b = 1;
    i = 2;

    while (i < n + 1) {
        temp = a + b;
        a = b;
        b = temp;
        i = i + 1;
    }

    return b;
}

result = fibonacci(10);
