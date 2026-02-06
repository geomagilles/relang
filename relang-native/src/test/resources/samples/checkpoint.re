fn process(x: Int): Int {
    let result = x * 2;
    checkpoint;
    result = result + 10;
    return result;
}

let output = process(5);
