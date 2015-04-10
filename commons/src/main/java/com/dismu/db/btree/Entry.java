package com.dismu.db.btree;

public class Entry<KeyT extends IKey, ValueT extends IValue> {
    public KeyT key;
    public ValueT value;

    public Entry(KeyT key, ValueT value) {
        this.key = key;
        this.value = value;
    }
}
