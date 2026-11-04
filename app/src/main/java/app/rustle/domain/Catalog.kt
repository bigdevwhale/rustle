package app.rustle.domain

/** A line in both languages. Catalogue texts live in code; screen texts live in resources. */
class L(val en: String, val ru: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/** What a find looks like and what it is good for in the workshop. Food shapes can also be eaten. */
enum class Shape(val food: Boolean = false) {
    BUTTON, SCREW, BEAD, SPRING, COIN, KEY, THREAD, PIN, LED, FEATHER, SCRAP, GEM, RING, MAGNET, STAR,
    CRUMB(true), RAISIN(true), SUGAR(true), SEED(true), HONEY(true);

    val label: L get() = when (this) {
        BUTTON -> L("button", "пуговица"); SCREW -> L("screw", "винтик"); BEAD -> L("bead", "бусина")
        SPRING -> L("spring", "пружинка"); COIN -> L("coin", "монетка"); KEY -> L("key", "ключик")
        THREAD -> L("thread", "нитка"); PIN -> L("pin", "булавка"); LED -> L("LED", "светодиод")
        FEATHER -> L("feather", "пёрышко"); SCRAP -> L("scrap of paper", "клочок бумаги"); GEM -> L("glass", "стёклышко")
        RING -> L("ring", "колечко"); MAGNET -> L("magnet", "магнитик"); STAR -> L("sequin", "блёстка")
        CRUMB -> L("rusk", "сухарик"); RAISIN -> L("raisin", "изюм"); SUGAR -> L("sugar", "сахар")
        SEED -> L("seed", "семечко"); HONEY -> L("honey", "мёд")
    }
}

/**
 * The districts inside the phone. Each is reached with a tool made in the workshop; the port is open
 * from the first day.
 */
enum class District(val name_: L, val where: L, val tool: String?) {
    PORT(L("The Port", "Разъём"), L("by the charging port, where the draught comes in", "у зарядного разъёма, откуда тянет сквозняком"), null),
    CELLAR(L("Battery Cellar", "Батарейный погреб"), L("under the battery: warm, dark, smells of raisins", "под батареей: тепло, темно, пахнет изюмом"), "lantern"),
    BRIDGE(L("Ribbon Bridge", "Шлейфовый мост"), L("a flat cable over the gap between the boards", "плоский шлейф над пропастью между платами"), "rope"),
    GORGE(L("Speaker Gorge", "Динамиковое ущелье"), L("loud: every ring echoes for an hour", "громко: каждый звонок эхом гуляет целый час"), "earmuffs"),
    DOME(L("The Observatory", "Обсерватория"), L("under the camera glass, where the sky shows through", "под стеклом камеры, где видно небо"), "ladder");
}

/** A small thing brought back from an outing. [food] is how much it fills a belly (0 for things). */
class Find(val key: String, val name: L, val shape: Shape, val color: Int, val rare: Int, val district: District, val food: Int = 0)

/** What the workshop makes. */
enum class Kind { TOOL, HOME, WEAR }

/**
 * [slot] is where it goes: for clothes the part of him it covers (one thing per slot), for furniture the
 * place in the nook it stands (one thing per place, chosen in the workshop); null stands or hangs on its own.
 */
class Build(val key: String, val kind: Kind, val name: L, val note: L, val cost: Map<Shape, Int>, val slot: Slot? = null)

enum class Slot(val title: L) {
    HEAD(L("Head", "Голова")), NECK(L("Neck", "Шея")), EYES(L("Eyes", "Глаза")), BACK(L("Back", "Спина")),
    WALL(L("Wall", "Стена")), SILL_L(L("Window sill, left", "Подоконник слева")), SILL_R(L("Window sill, right", "Подоконник справа")),
    FRONT_L(L("Front corner, left", "Передний угол слева")), FRONT_R(L("Front corner, right", "Передний угол справа")),
}

/** A dish from the kitchen corner: eats its ingredients and fills more than they would alone. */
class Dish(val key: String, val name: L, val cost: Map<Shape, Int>, val full: Int, val mood: Int)

object Catalog {
    private fun f(key: String, en: String, ru: String, shape: Shape, color: Long, rare: Int, d: District, food: Int = 0) =
        Find(key, L(en, ru), shape, (color or 0xFF000000).toInt(), rare, d, food)

    val finds: List<Find> = listOf(
        // The port: buttons, pins, threads, crumbs off the bottom of a pocket.
        f("pin_brass", "Brass pin", "Латунная булавка", Shape.PIN, 0xE0B050, 1, District.PORT),
        f("pin_silver", "Silver pin", "Серебристая булавка", Shape.PIN, 0xC8CED6, 1, District.PORT),
        f("button_red", "Red button", "Красная пуговица", Shape.BUTTON, 0xD24A3A, 1, District.PORT),
        f("button_shirt", "Shirt button", "Пуговица от рубашки", Shape.BUTTON, 0xF2EEE2, 1, District.PORT),
        f("button_coat", "Coat button", "Пуговица от пальто", Shape.BUTTON, 0x7A4A2A, 2, District.PORT),
        f("screw_tiny", "Tiny screw", "Крошечный винтик", Shape.SCREW, 0xB8C0C8, 1, District.PORT),
        f("screw_gold", "Gilt screw", "Позолоченный винтик", Shape.SCREW, 0xF0C050, 2, District.PORT),
        f("thread_red", "Red thread", "Красная нитка", Shape.THREAD, 0xD24A3A, 1, District.PORT),
        f("thread_blue", "Blue thread", "Синяя нитка", Shape.THREAD, 0x4A72C8, 1, District.PORT),
        f("led_red", "Red LED", "Красный светодиод", Shape.LED, 0xF05040, 1, District.PORT),
        f("sugar", "Grain of sugar", "Крупинка сахара", Shape.SUGAR, 0xFFFFFF, 1, District.PORT, 10),
        f("rusk", "Rusk", "Сухарик", Shape.CRUMB, 0xD8A060, 1, District.PORT, 20),
        f("scrap_ticket", "Bit of a tram ticket", "Обрывок трамвайного билета", Shape.SCRAP, 0xE8D890, 2, District.PORT),
        f("key_diary", "Diary key", "Ключик от дневника", Shape.KEY, 0xE0B050, 3, District.PORT),

        // The battery cellar: warm metal, coins and dried fruit.
        f("coin_copper", "Copper coin", "Медная монетка", Shape.COIN, 0xD08040, 1, District.CELLAR),
        f("coin_foreign", "Foreign coin", "Заморская монетка", Shape.COIN, 0xD8DCE0, 2, District.CELLAR),
        f("raisin", "Raisin", "Изюминка", Shape.RAISIN, 0x6A3050, 1, District.CELLAR, 30),
        f("honey", "Drop of honey", "Капля мёда", Shape.HONEY, 0xF0A830, 2, District.CELLAR, 45),
        f("rusk_rye", "Rye rusk", "Ржаной сухарик", Shape.CRUMB, 0x9A6038, 1, District.CELLAR, 25),
        f("magnet", "Fridge magnet", "Магнитик", Shape.MAGNET, 0xD24A3A, 1, District.CELLAR),
        f("magnet_shoe", "Horseshoe magnet", "Магнит-подковка", Shape.MAGNET, 0x4A72C8, 2, District.CELLAR),
        f("spring_coil", "Spring", "Пружинка", Shape.SPRING, 0xB8C0C8, 1, District.CELLAR),
        f("led_amber", "Amber LED", "Янтарный светодиод", Shape.LED, 0xF0A030, 1, District.CELLAR),
        f("screw_black", "Black screw", "Чёрный винтик", Shape.SCREW, 0x505860, 1, District.CELLAR),
        f("key_brass", "Brass key", "Латунный ключик", Shape.KEY, 0xD8A848, 2, District.CELLAR),
        f("gem_amber", "Drop of amber", "Капелька янтаря", Shape.GEM, 0xF09A28, 3, District.CELLAR),

        // The ribbon bridge: things that blow about, threads, feathers, beads.
        f("thread_gold", "Gold thread", "Золотая нитка", Shape.THREAD, 0xF0C050, 2, District.BRIDGE),
        f("thread_green", "Green thread", "Зелёная нитка", Shape.THREAD, 0x58A050, 1, District.BRIDGE),
        f("glass_bottle", "Bit of bottle glass", "Бутылочное стёклышко", Shape.GEM, 0x6AB88A, 1, District.BRIDGE),
        f("feather_sparrow", "Sparrow feather", "Воробьиное пёрышко", Shape.FEATHER, 0x9A7450, 1, District.BRIDGE),
        f("feather_jay", "Jay feather", "Пёрышко сойки", Shape.FEATHER, 0x4A8AD8, 2, District.BRIDGE),
        f("bead_glass", "Glass bead", "Стеклянная бусина", Shape.BEAD, 0x9AD8E0, 1, District.BRIDGE),
        f("bead_red", "Red bead", "Красная бусина", Shape.BEAD, 0xD24A3A, 1, District.BRIDGE),
        f("bead_pearl", "Tiny pearl", "Жемчужинка", Shape.BEAD, 0xF4F0E8, 3, District.BRIDGE),
        f("spring_long", "Long spring", "Длинная пружинка", Shape.SPRING, 0xD8B060, 1, District.BRIDGE),
        f("pin_safety", "Safety pin", "Английская булавка", Shape.PIN, 0xD0D4DA, 1, District.BRIDGE),
        f("button_four", "Four-hole button", "Пуговица с четырьмя дырочками", Shape.BUTTON, 0x58A050, 1, District.BRIDGE),
        f("seed_sunflower", "Sunflower seed", "Семечко", Shape.SEED, 0x404040, 1, District.BRIDGE, 20),
        f("scrap_letter", "Corner of a letter", "Уголок письма", Shape.SCRAP, 0xF2EEE2, 2, District.BRIDGE),

        // The speaker gorge: shiny things shaken loose by the ringing.
        f("star_sequin", "Sequin", "Пайетка", Shape.STAR, 0xE070C0, 1, District.GORGE),
        f("star_gold", "Gold star", "Золотая звёздочка", Shape.STAR, 0xF8D050, 2, District.GORGE),
        f("ring_tin", "Tin ring", "Жестяное колечко", Shape.RING, 0xB8C0C8, 1, District.GORGE),
        f("ring_silver", "Silver ring", "Серебряное колечко", Shape.RING, 0xE8ECF0, 3, District.GORGE),
        f("bead_black", "Black bead", "Чёрная бусина", Shape.BEAD, 0x303038, 1, District.GORGE),
        f("spring_bell", "Doorbell spring", "Пружинка от звонка", Shape.SPRING, 0xE0B050, 2, District.GORGE),
        f("coin_token", "Metro token", "Жетон метро", Shape.COIN, 0xC8A050, 2, District.GORGE),
        f("feather_owl", "Owl feather", "Совиное пёрышко", Shape.FEATHER, 0xC8A878, 2, District.GORGE),
        f("glass_frost", "Frosted glass", "Матовое стёклышко", Shape.GEM, 0xC8D8E0, 1, District.GORGE),
        f("led_green", "Green LED", "Зелёный светодиод", Shape.LED, 0x60E070, 1, District.GORGE),
        f("sugar_cube", "Sugar lump", "Кусочек рафинада", Shape.SUGAR, 0xFFFFFF, 2, District.GORGE, 25),
        f("scrap_music", "Bit of sheet music", "Клочок нотной тетради", Shape.SCRAP, 0xE8E0C8, 2, District.GORGE),

        // The observatory: glass, stars and moonlit things.
        f("gem_lens", "Chip of a lens", "Осколок линзы", Shape.GEM, 0x88C8F0, 2, District.DOME),
        f("gem_violet", "Violet crumb", "Аметистовая крошка", Shape.GEM, 0xA070D8, 3, District.DOME),
        f("star_fallen", "Fallen star", "Упавшая звезда", Shape.STAR, 0xFFF0A0, 3, District.DOME),
        f("ring_gold", "Gold ring", "Золотое колечко", Shape.RING, 0xF8C840, 3, District.DOME),
        f("sugar_star", "Star sugar", "Звёздный сахар", Shape.SUGAR, 0xFFF4C0, 2, District.DOME, 40),
        f("feather_white", "White feather", "Белое пёрышко", Shape.FEATHER, 0xF4F4F0, 1, District.DOME),
        f("key_star", "Star key", "Звёздный ключик", Shape.KEY, 0xC8D8F8, 3, District.DOME),
        f("bead_moon", "Moon bead", "Лунная бусина", Shape.BEAD, 0xD8E0F0, 2, District.DOME),
        f("led_white", "White LED", "Белый светодиод", Shape.LED, 0xF0F4FF, 1, District.DOME),
        f("seed_moon", "Moon seed", "Лунное семечко", Shape.SEED, 0xB8C0E0, 2, District.DOME, 30),
        f("scrap_chart", "Piece of a star chart", "Кусочек звёздной карты", Shape.SCRAP, 0x30406A, 2, District.DOME),
    )

    private val byKey = finds.associateBy { it.key }
    fun find(key: String) = byKey[key]

    fun of(d: District) = finds.filter { it.district == d }

    private fun b(key: String, kind: Kind, en: String, ru: String, noteEn: String, noteRu: String, vararg cost: Pair<Shape, Int>, slot: Slot? = null) =
        Build(key, kind, L(en, ru), L(noteEn, noteRu), cost.toMap(), slot)

    val builds: List<Build> = listOf(
        b("lantern", Kind.TOOL, "Lantern", "Фонарик", "Opens the Battery Cellar.", "Открывает Батарейный погреб.", Shape.LED to 1, Shape.SCREW to 2, Shape.THREAD to 1),
        b("rope", Kind.TOOL, "Rope", "Верёвка", "Opens the Ribbon Bridge.", "Открывает Шлейфовый мост.", Shape.THREAD to 3, Shape.SPRING to 1),
        b("earmuffs", Kind.TOOL, "Earmuffs", "Наушники-грелки", "Opens the Speaker Gorge.", "Открывают Динамиковое ущелье.", Shape.BUTTON to 2, Shape.FEATHER to 2, Shape.SPRING to 1),
        b("ladder", Kind.TOOL, "Ladder", "Лесенка", "Opens the Observatory.", "Открывает Обсерваторию.", Shape.PIN to 4, Shape.SCREW to 2, Shape.STAR to 2),

        b("bed", Kind.HOME, "Matchbox bed", "Кровать из коробка", "Good nights cheer him up more.", "После хорошей ночи настроение выше.", Shape.BUTTON to 2, Shape.THREAD to 2),
        b("rug", Kind.HOME, "Knitted rug", "Вязаный коврик", "Warmer feet.", "Лапам теплее.", Shape.THREAD to 4),
        b("lamp", Kind.HOME, "LED lamp", "Лампа", "Light in the dark: no candle needed.", "В темноте светло, свечка больше не нужна.", Shape.LED to 1, Shape.SPRING to 1, Shape.SCREW to 1),
        b("shelf", Kind.HOME, "Shelf", "Полочка", "For the finest finds.", "Для лучших находок.", Shape.PIN to 3, Shape.SCREW to 2),
        b("picture", Kind.HOME, "Picture", "Картинка", "A meadow on the wall.", "Луг на стене.", Shape.SCRAP to 1, Shape.PIN to 1, slot = Slot.WALL),
        b("map", Kind.HOME, "Map of the phone", "Карта телефона", "Every district he has walked, pinned to the wall.", "На ней все места, где он уже побывал.", Shape.SCRAP to 2, Shape.PIN to 2, slot = Slot.WALL),
        b("mirror", Kind.HOME, "Round mirror", "Круглое зеркальце", "He waves at himself every morning.", "По утрам он машет своему отражению.", Shape.GEM to 1, Shape.RING to 1, Shape.SCREW to 1, slot = Slot.WALL),
        b("plant", Kind.HOME, "Sprout in a thimble", "Росток в напёрстке", "Grows a little every day.", "Каждый день чуть подрастает.", Shape.SEED to 1, Shape.BEAD to 1, slot = Slot.SILL_R),
        b("musicbox", Kind.HOME, "Music box", "Шкатулка с музыкой", "A tiny tune when the lid is up.", "Откроешь крышку — играет крошечная мелодия.", Shape.SPRING to 2, Shape.KEY to 1, Shape.COIN to 1, slot = Slot.SILL_R),
        b("telescope", Kind.HOME, "Spyglass", "Подзорная трубка", "Rare finds turn up more often.", "Редкие находки попадаются чаще.", Shape.GEM to 1, Shape.RING to 1, Shape.SCREW to 2, slot = Slot.SILL_L),
        b("snowglobe", Kind.HOME, "Snow globe", "Снежный шар", "Snows inside whenever you shake the phone.", "Внутри идёт снег, когда трясёшь телефон.", Shape.GEM to 1, Shape.SUGAR to 1, Shape.BUTTON to 1, slot = Slot.SILL_L),
        b("armchair", Kind.HOME, "Thimble armchair", "Кресло из напёрстка", "For long evenings.", "Для долгих вечеров.", Shape.BUTTON to 3, Shape.THREAD to 2, Shape.SCREW to 1, slot = Slot.FRONT_L),
        b("gramophone", Kind.HOME, "Gramophone", "Граммофон", "Plays the gorge's echoes back.", "Проигрывает эхо из ущелья.", Shape.SPRING to 2, Shape.COIN to 1, Shape.STAR to 1, slot = Slot.FRONT_L),
        b("pouf", Kind.HOME, "Pincushion pouf", "Пуфик-игольница", "Soft, if you mind the pins.", "Мягкий, если не считать булавок.", Shape.THREAD to 2, Shape.PIN to 3, slot = Slot.FRONT_L),
        b("fern", Kind.HOME, "Fern in a cap", "Папоротник в колпачке", "Grows from a moon seed.", "Вырос из лунного семечка.", Shape.SEED to 2, Shape.RING to 1, slot = Slot.FRONT_R),
        b("radio", Kind.HOME, "Crystal radio", "Детекторный приёмник", "Picks up the other phones nearby.", "Ловит соседние телефоны.", Shape.MAGNET to 1, Shape.LED to 1, Shape.SPRING to 1, Shape.COIN to 1, slot = Slot.FRONT_R),
        b("aquarium", Kind.HOME, "Bottle-cap aquarium", "Аквариум в пробке", "One bead fish, very calm.", "Одна рыбка-бусина, очень спокойная.", Shape.GEM to 1, Shape.BEAD to 2, slot = Slot.FRONT_R),
        b("garland", Kind.HOME, "Garland", "Гирлянда", "Glimmers in the evening.", "Мерцает по вечерам.", Shape.STAR to 3, Shape.THREAD to 1),
        b("clock", Kind.HOME, "Pocket clock", "Часики", "He always knows when to come home.", "С ними он не опаздывает домой.", Shape.COIN to 1, Shape.SPRING to 2, Shape.KEY to 1),

        b("cap", Kind.WEAR, "Nightcap", "Ночной колпак", "Sleepy and striped.", "Сонный и полосатый.", Shape.THREAD to 2, Shape.BEAD to 1, slot = Slot.HEAD),
        b("beret", Kind.WEAR, "Beret", "Берет", "For an artist of the board.", "Для художника с платы.", Shape.THREAD to 2, Shape.BUTTON to 1, slot = Slot.HEAD),
        b("bobble", Kind.WEAR, "Bobble hat", "Шапка с помпоном", "Knitted from three threads.", "Связана из трёх ниток.", Shape.THREAD to 3, Shape.BEAD to 1, slot = Slot.HEAD),
        b("acorn", Kind.WEAR, "Acorn cap", "Шапочка-жёлудь", "Found, not made. Almost.", "Найдена, а не сделана. Почти.", Shape.SEED to 1, Shape.CRUMB to 1, slot = Slot.HEAD),
        b("crown", Kind.WEAR, "Foil crown", "Корона из фольги", "House spirit material.", "Для будущего домового.", Shape.COIN to 2, Shape.STAR to 1, slot = Slot.HEAD),
        b("wreath", Kind.WEAR, "Flower wreath", "Веночек", "Sequins for petals.", "Вместо лепестков — пайетки.", Shape.STAR to 2, Shape.THREAD to 1, Shape.SEED to 1, slot = Slot.HEAD),
        b("bow", Kind.WEAR, "Ribbon bow", "Бантик", "Tied twice for luck.", "Завязан дважды, на удачу.", Shape.THREAD to 2, slot = Slot.HEAD),
        b("tophat", Kind.WEAR, "Top hat", "Цилиндр", "Out of a marker cap.", "Из колпачка от фломастера.", Shape.RING to 1, Shape.BUTTON to 1, Shape.THREAD to 1, slot = Slot.HEAD),
        b("bandana", Kind.WEAR, "Bandana", "Бандана", "For bold expeditions.", "Для смелых вылазок.", Shape.SCRAP to 1, Shape.THREAD to 1, slot = Slot.HEAD),
        b("wizard", Kind.WEAR, "Stargazer's hat", "Колпак звездочёта", "From a corner of a star chart.", "Из уголка звёздной карты.", Shape.SCRAP to 2, Shape.STAR to 2, slot = Slot.HEAD),
        b("propeller", Kind.WEAR, "Propeller cap", "Кепка с пропеллером", "Spins when he runs.", "Крутится, когда он бежит.", Shape.BUTTON to 1, Shape.SPRING to 1, Shape.PIN to 2, slot = Slot.HEAD),
        b("scarf", Kind.WEAR, "Scarf", "Шарфик", "Warm when the battery is low.", "Греет, когда батарея садится.", Shape.THREAD to 3, slot = Slot.NECK),
        b("bowtie", Kind.WEAR, "Bow tie", "Бабочка", "For visits and holidays.", "Для гостей и праздников.", Shape.THREAD to 1, Shape.BUTTON to 1, slot = Slot.NECK),
        b("bell", Kind.WEAR, "Bell on a ribbon", "Колокольчик на ленте", "You hear him coming home.", "Слышно, как он идёт домой.", Shape.THREAD to 1, Shape.COIN to 1, Shape.SPRING to 1, slot = Slot.NECK),
        b("beads", Kind.WEAR, "Bead necklace", "Бусы", "Every bead from a different place.", "Каждая бусина из другого места.", Shape.BEAD to 4, Shape.THREAD to 1, slot = Slot.NECK),
        b("goggles", Kind.WEAR, "Goggles", "Очки-консервы", "For sunbathing under the camera.", "Чтобы загорать под камерой.", Shape.GEM to 1, Shape.RING to 2, slot = Slot.EYES),
        b("glasses", Kind.WEAR, "Round glasses", "Круглые очки", "For reading finds' labels.", "Чтобы читать этикетки на находках.", Shape.RING to 2, Shape.PIN to 1, slot = Slot.EYES),
        b("monocle", Kind.WEAR, "Monocle", "Монокль", "Very serious. On a chain.", "Очень солидно. На цепочке.", Shape.RING to 1, Shape.GEM to 1, slot = Slot.EYES),
        b("backpack", Kind.WEAR, "Backpack", "Рюкзачок", "One more find on outings of half an hour or longer.", "С вылазки от получаса — на одну находку больше.", Shape.SCRAP to 2, Shape.THREAD to 1, Shape.BUTTON to 1, slot = Slot.BACK),
        b("cape", Kind.WEAR, "Cape", "Плащ", "Flaps on the ribbon bridge.", "Развевается на шлейфовом мосту.", Shape.SCRAP to 2, Shape.THREAD to 2, Shape.BUTTON to 1, slot = Slot.BACK),
    )

    fun build(key: String) = builds.firstOrNull { it.key == key }

    /** What stands in each place of the nook: the chosen thing, or the first one made for it. */
    fun placed(built: Set<String>, place: Map<String, String>): Set<String> = buildSet {
        for (b in builds) if (b.kind == Kind.HOME && b.slot == null && b.key in built) add(b.key)
        for (sl in listOf(Slot.WALL, Slot.SILL_L, Slot.SILL_R, Slot.FRONT_L, Slot.FRONT_R)) {
            val pick = place[sl.name]?.takeIf { it in built } ?: builds.firstOrNull { it.slot == sl && it.key in built }?.key
            pick?.let { add(it) }
        }
        for (b in builds) if (b.kind == Kind.TOOL && b.key in built) add(b.key)
    }

    val dishes: List<Dish> = listOf(
        Dish("tea", L("Sweet tea", "Сладкий чай"), mapOf(Shape.SUGAR to 2), 15, 15),
        Dish("porridge", L("Seed porridge", "Каша из семечек"), mapOf(Shape.SEED to 2), 50, 5),
        Dish("pie", L("Raisin pie", "Пирожок с изюмом"), mapOf(Shape.CRUMB to 1, Shape.RAISIN to 1), 60, 10),
        Dish("honeycake", L("Honey cake", "Медовик"), mapOf(Shape.HONEY to 1, Shape.CRUMB to 1, Shape.SUGAR to 1), 90, 25),
    )

    /** How many of a shape are in hand. */
    fun have(inv: Map<String, Int>, s: Shape) = inv.entries.sumOf { (k, n) -> if (find(k)?.shape == s) n else 0 }

    fun canPay(inv: Map<String, Int>, cost: Map<Shape, Int>) = cost.all { (s, n) -> have(inv, s) >= n }

    /** Takes [cost] from the inventory, plainest things first (commons before rares). */
    fun pay(inv: Map<String, Int>, cost: Map<Shape, Int>): Map<String, Int> {
        val out = inv.toMutableMap()
        for ((s, n) in cost) {
            var left = n
            for (fd in finds.filter { it.shape == s }.sortedBy { it.rare }) {
                val k = out[fd.key] ?: 0
                val take = minOf(k, left)
                if (take > 0) { out[fd.key] = k - take; left -= take }
                if (left == 0) break
            }
            require(left == 0) { "not enough $s" }
        }
        return out.filterValues { it > 0 }
    }
}
