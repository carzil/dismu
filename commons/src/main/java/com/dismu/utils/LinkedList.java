package com.dismu.utils;


public class LinkedList<T> {
    public static class Node<T> {
        public Node<T> next;
        public Node<T> prev;
        public T value;

        public Node(T value) {
            this.value = value;
        }
    }

    private Node<T> first = null;
    private Node<T> last = null;
    private int size = 0;

    public LinkedList() {

    }

    public void insertFirst(Node<T> newFirst) {
        if (first == null) {
            last = first = newFirst;
        } else {
            newFirst.next = first;
            first = newFirst;
            first.prev = newFirst;
        }
        size++;
    }

    public void insertLast(Node<T> newLast) {
        if (last == null) {
            last = first = newLast;
        } else {
            last.next = newLast;
            last = newLast;
        }
        size++;
    }

    public void insertAfter(Node<T> e, Node<T> prev) {
        Node<T> next = prev.next;
        if (next != null) {
            next.prev = e;
        } else {
            prev.next = e;
            last = e;
        }
        size++;
    }

    public void insertBefore(Node<T> e, Node<T> next) {
        Node<T> prev = next.prev;
        if (prev == null) {
            first = e;
        } else {
            prev.next = e;
        }
        size++;
    }

    public void remove(Node<T> e) {
        if (e.prev == null) {
            first = first.next;
            first.prev = null;
        } else if (e.next == null) {
            last = last.prev;
            last.next = null;
        } else {
            Node<T> prev = e.prev;
            Node<T> next = e.next;
            prev.next = next;
            next.prev = prev;
        }
    }

    public LinkedList<T> merge(LinkedList<T> otherList) {
        LinkedList<T> newList = new LinkedList<>();
        if (otherList.last != null) {
            otherList.first.prev = last;
            last.next = otherList.first;
            newList.first = first;
            newList.last = otherList.last;
            return newList;
        } else {
            return this;
        }
    }

    public void add(T e) {
        Node<T> node = new Node<T>(e);
        insertLast(node);
    }

    public Node<T> getFirst() {
        return first;
    }

    public int size() {
        return size;
    }
}
