// Checkpoint inside a loop showcasing v0.1 syntax
fn sum_with_checkpoints(n: Int): Int {
    let total = 0;
    for i in 1..=n {
        total = total + i;
        checkpoint;
    }
    total
}

let result = sum_with_checkpoints(5);
