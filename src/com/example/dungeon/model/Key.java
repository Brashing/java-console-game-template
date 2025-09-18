package com.example.dungeon.model;

public class Key extends Item {
    public Key(String name) {
        super(name);
    }

    @Override
    public void apply(GameState ctx) {
        System.out.println("Ключ звенит. Возможно, где-то есть дверь...");
    }
}
