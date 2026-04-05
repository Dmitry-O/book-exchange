package com.example.bookexchange.support;

public final class FixtureNumbers {

    private static final int BLOCK_SIZE = 1_000;

    private FixtureNumbers() {
    }

    public static int auth(int slot) {
        return slot(1, slot);
    }

    public static int book(int slot) {
        return slot(2, slot);
    }

    public static int user(int slot) {
        return slot(3, slot);
    }

    public static int request(int slot) {
        return slot(4, slot);
    }

    public static int offer(int slot) {
        return slot(5, slot);
    }

    public static int history(int slot) {
        return slot(6, slot);
    }

    public static int report(int slot) {
        return slot(7, slot);
    }

    public static int security(int slot) {
        return slot(8, slot);
    }

    public static int adminUser(int slot) {
        return slot(9, slot);
    }

    public static int adminBook(int slot) {
        return slot(10, slot);
    }

    public static int adminExchange(int slot) {
        return slot(11, slot);
    }

    public static int adminReport(int slot) {
        return slot(12, slot);
    }

    private static int slot(int block, int slot) {
        if (slot < 1 || slot >= BLOCK_SIZE) {
            throw new IllegalArgumentException("slot must be between 1 and " + (BLOCK_SIZE - 1));
        }

        return (block * BLOCK_SIZE) + slot;
    }
}
