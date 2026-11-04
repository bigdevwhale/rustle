package app.rustle.domain

/** Who lives in the phone. Each kind has its own ears, tail and extras; the fur colour is the owner's choice. */
enum class Species(val title: L, val note: L, val body: Int, val accent: Int) {
    MOUSE(L("Rustler", "Шуршик"), L("Round ears and a plume of a tail. Rustles in the cables.", "Круглые уши и хвост-плюмаж. Шуршит в проводах."), 0xC8946A, 0xF2DCC0),
    HEDGEHOG(L("Prickle", "Ёжик"), L("Spines on the back, a soft tummy. Sleeps curled up.", "Иголки на спине, мягкое пузико. Спит клубком."), 0x8A6A52, 0xF0DCC0),
    BUNNY(L("Lop", "Ушастик"), L("Long ears that hear the speaker before it rings.", "Длинные уши слышат динамик раньше звонка."), 0xE8DED0, 0xFFF4EC),
    MOTH(L("Moth", "Мотылёк"), L("Feathery feelers and soft wings. Loves the lamp.", "Перистые усики и мягкие крылья. Любит лампу."), 0xB8A890, 0xF4ECDA),
    FOX(L("Foxling", "Лисёнок"), L("Pointed ears and a tail with a white tip.", "Острые уши и хвост с белым кончиком."), 0xD87A40, 0xFFF0E0),
    OWL(L("Owlet", "Совёнок"), L("A round face, a tiny beak and wings to hug with.", "Круглое лицо, крошечный клюв и крылья, чтобы обниматься."), 0x9A7A5A, 0xF0E0C8),
    CAT(L("Kit", "Котик"), L("Cat's ears and a thin tail with a question mark at the end.", "Кошачьи уши и тонкий хвост вопросиком."), 0x8A8A92, 0xE8E8EE),
    BAT(L("Flitter", "Нетопырь"), L("Huge ears and folded wings. Hangs about at night.", "Огромные уши и сложенные крылья. Ночью оживляется."), 0x6A5A6A, 0xD8C8D0);

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: MOUSE
    }
}

/** A pattern over the fur. */
enum class Marks(val title: L) {
    NONE(L("Plain", "Однотонный")),
    SPOTS(L("Spots", "Пятнышки")),
    STRIPES(L("Stripes", "Полоски")),
    PATCH(L("Eye patch", "Пятно у глаза")),
    MASK(L("Mask", "Маска")),
    SOCKS(L("Socks", "Носочки"));

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: NONE
    }
}
