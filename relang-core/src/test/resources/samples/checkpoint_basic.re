// Basic checkpoint/resume showcasing v0.1 syntax
fn process(x: Int): Int {
    let result = x * 2;
    checkpoint;
    result + 10
}

let output = process(5);
