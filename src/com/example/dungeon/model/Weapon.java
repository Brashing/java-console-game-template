package com.example.dungeon.model;

public class Weapon extends Item {
    private final int bonus;

    public Weapon(String name, int bonus) {
        super(name);
        this.bonus = bonus;
    }

    @Override
    public void apply(GameState ctx) {
        var p = ctx.getPlayer();
        p.setAttack(p.getAttack() + bonus);
        System.out.println("Оружие экипировано. Атака теперь: " + p.getAttack());
        p.getInventory().remove(this);
    }
}
