package com.example.dungeon.core;

// Импорт всех моделей из пакета model

import com.example.dungeon.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class Game {
    // Объект GameState хранит состояние игры, игрока и текущую комнату
    private final GameState state = new GameState();
    // Карта команд (ключ — название команды, значение — реализация команды)
    private final Map<String, Command> commands = new LinkedHashMap<>();

    static {
        // Статический блок — вызывается при загрузке класса, фиксирует метаданные
        WorldInfo.touch("Game");
    }

    public Game() {
        registerCommands(); // Настраиваем команды
        bootstrapWorld(); // Создаем стартовые комнаты и связи вручную
    }

    /**
     * Создает первичные комнаты, устанавливает связи и наполняет иллюстрационный мир
     */
    private void bootstrapWorld() {
        Player hero = new Player("Герой", 20, 5);
        // Создаём игрока и добавляем его в состояние
        state.setPlayer(hero);

        // Создаем три комнаты с описанием
        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");

        // Устанавливаем связи
        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        cave.getNeighbors().put("west", forest);

        // В одной из комнат добавляем предмет
        forest.getItems().add(new Potion("Малое зелье", 5));
        // И в той же комнате создаем монстра
        forest.setMonster(new Monster("Волк", 1, 8));

        // Устанавливаем текущую комнату — стартовую
        state.setCurrent(square);
    }

    /**
     * Регистрация команд — для управления игрой через консоль
     */
    private void registerCommands() {
        // Показывает все команды
        commands.put("help", (ctx, a) -> System.out.println("Команды: " + String.join(", ", commands.keySet())));
        // Команда для проверки памяти
        commands.put("gc-stats", (ctx, a) -> {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory(), used = total - free;
            System.out.println("Память: used=" + used + " free=" + free + " total=" + total);
        });
        // Для демонстрации сборщика мусора
        commands.put("alloc", (ctx, a) -> {
            System.out.println("Создаю большой массив для демонстрации GC...");
            int[] arr = new int[10_000_000];
            for (int i = 0; i < arr.length; i++) arr[i] = i;
            System.out.println("Массив создан");
        });
        // Команда для описания текущей комнаты
        commands.put("look", (ctx, a) -> System.out.println(ctx.getCurrent().describe()));
        // Перемещение игрока
        commands.put("move", (ctx, a) -> {
            if (a.isEmpty()) throw new InvalidCommandException("Укажите направление (north, south, east, west)");
            String dir = a.get(0).toLowerCase();
            Room current = ctx.getCurrent();
            Room next = current.getNeighbors().get(dir);
            if (next == null) throw new InvalidCommandException("Нет выхода в этом направлении: " + dir);
            ctx.setCurrent(next); // обновляем текущую комнату
            System.out.println("Вы перешли в: " + next.getName());
            System.out.println(next.describe());
        });
        // Взять предмет из комнаты
        commands.put("take", (ctx, a) -> {
            if (a.isEmpty()) throw new InvalidCommandException("Укажите предмет для взятия");
            String itemName = String.join(" ", a);
            Room current = ctx.getCurrent();
            Optional<Item> itemOpt = current.getItems().stream().filter(i -> i.getName().equalsIgnoreCase(itemName)).findFirst();
            if (itemOpt.isEmpty()) throw new InvalidCommandException("Предмет не найден: " + itemName);
            Item item = itemOpt.get();
            ctx.getPlayer().getInventory().add(item);
            current.getItems().remove(item);
            System.out.println("Взято: " + item.getName());
        });
        // Вывод инвентаря с группировкой по типам
        commands.put("inventory", (ctx, a) -> {
            Map<String, List<Item>> grouped = ctx.getPlayer().getInventory().stream().collect(Collectors.groupingBy(Item::getClassName));
            if (grouped.isEmpty()) {
                System.out.println("Инвентарь пуст.");
                return;
            }
            grouped.forEach((type, items) -> {
                long count = items.size();
                System.out.println("- " + type + " (" + count + "):");
                items.forEach(i -> System.out.println("  - " + i.getName()));
            });
        });
        // Использование предмета
        commands.put("use", (ctx, a) -> {
            if (a.isEmpty()) throw new InvalidCommandException("Укажите предмет для использования");
            String itemName = String.join(" ", a);
            Player p = ctx.getPlayer();
            Optional<Item> itemOpt = p.getInventory().stream().filter(i -> i.getName().equalsIgnoreCase(itemName)).findFirst();
            if (itemOpt.isEmpty()) throw new InvalidCommandException("Предмет не найден в инвентаре: " + itemName);
            Item item = itemOpt.get();
            item.apply(ctx);
        });
        // Бой с монстром
        commands.put("fight", (ctx, a) -> {
            Room current = ctx.getCurrent();
            Monster m = current.getMonster();
            if (m == null) throw new InvalidCommandException("Здесь нет монстров для боя.");
            Player p = ctx.getPlayer();
            System.out.println("Бой с монстром: " + m.getName() + " (ур. " + m.getLevel() + ", HP: " + m.getHp() + ")");
            while (p.getHp() > 0 && m.getHp() > 0) {
                // Игрок атакует
                int damage = p.getAttack();
                m.setHp(m.getHp() - damage);
                System.out.println("Вы бьёте " + m.getName() + " на " + damage + ". HP монстра: " + Math.max(0, m.getHp()));
                if (m.getHp() <= 0) {
                    System.out.println("Вы победили монстра " + m.getName() + "!");
                    current.setMonster(null);
                    return;
                }
                // Монстр отвечает
                int dmg = Math.max(0, m.getLevel() * 2 - p.getAttack() / 2);
                p.setHp(p.getHp() - dmg);
                System.out.println("Монстр отвечает: " + dmg + ". Ваше HP: " + Math.max(0, p.getHp()));
                if (p.getHp() <= 0) {
                    System.out.println("Вы погибли. Игра окончена.");
                    System.exit(0);
                }
            }
        });

        // Сохранение
        commands.put("save", (ctx, a) -> SaveLoad.save(ctx));
        // Загрузка
        commands.put("load", (ctx, a) -> SaveLoad.load(ctx));
        // Очки
        commands.put("scores", (ctx, a) -> SaveLoad.printScores());
        // Выход
        commands.put("exit", (ctx, a) -> {
            System.out.println("Пока!");
            System.exit(0);
        });
    }

    public void run() {
        System.out.println("DungeonMini (TEMPLATE). 'help' — команды.");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> parts = Arrays.asList(line.split("\\s+"));
                String cmd = parts.getFirst().toLowerCase(Locale.ROOT);
                List<String> args = parts.subList(1, parts.size());
                Command c = commands.get(cmd);
                try {
                    if (c == null) throw new InvalidCommandException("Неизвестная команда: " + cmd);
                    c.execute(state, args);
                    state.addScore(1);
                } catch (InvalidCommandException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Непредвиденная ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}
