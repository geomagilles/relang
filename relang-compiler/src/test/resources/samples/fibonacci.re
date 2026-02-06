fn fibonacci(n: Int): Int {
    if (n < 2) {
        return n;
    }

    let a = 0;
    let b = 1;
    let i = 2;

    while (i < n + 1) {
        let temp = a + b;
        a = b;
        b = temp;
        i = i + 1;
    }

    return b;
}

let result = fibonacci(10);
