package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import java.util.ArrayList;

/**
 * AST node that creates a product value from two child expressions.
 * Nested products are flattened: {@code (a & b) & c} becomes {@code (a, b, c)}.
 */
public class ReLangProductNode extends ReLangNode {
    @Child private ReLangNode left;
    @Child private ReLangNode right;

    public ReLangProductNode(ReLangNode left, ReLangNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var leftVal = left.executeGeneric(frame);
        var rightVal = right.executeGeneric(frame);

        // Flatten nested products: (a & b) & c -> (a, b, c)
        var components = new ArrayList<>();
        if (leftVal instanceof ReLangProduct lp) {
            for (var c : lp.getComponents()) components.add(c);
        } else {
            components.add(leftVal);
        }
        if (rightVal instanceof ReLangProduct rp) {
            for (var c : rp.getComponents()) components.add(c);
        } else {
            components.add(rightVal);
        }
        return new ReLangProduct(components.toArray());
    }
}
