fn sum_with_checkpoints(n) {
    total = 0;
    i = 1;

    while (i < n + 1) {
        total = total + i;
        checkpoint;
        i = i + 1;
    }

    return total;
}

result = sum_with_checkpoints(5);
