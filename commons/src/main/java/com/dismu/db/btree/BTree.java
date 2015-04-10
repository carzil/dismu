package com.dismu.db.btree;


import com.dismu.utils.LinkedList;

public class BTree<KeyT extends IKey, ValueT extends IValue> {
    private final static int FACTOR = 1024;
    private int size = 0;
    private Node root;

    private static class Node {
        public IKey key;
        public IValue value;
        public Node next;
        private LinkedList<Node> children = new LinkedList<>();


        public int size() {
            return children.size();
        }

        public LinkedList<Node> getChildren() {
            return children;
        }

        public void addChild(Node c) {
            children.add(c);
        }
    }

    public BTree() {
        root = new Node();
    }

    public void put(IKey key, IValue value) {

    }

    public int size() {
        return size;
    }
}
