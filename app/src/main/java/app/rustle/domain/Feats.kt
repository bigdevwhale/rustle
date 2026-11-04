package app.rustle.domain

import app.rustle.data.State

/** An achievement: a count read from the household and the number it has to reach. */
class Feat(val key: String, val group: Group, val name: L, val note: L, val goal: Int, val value: (State) -> Int) {
    fun progress(s: State) = value(s).coerceAtMost(goal)
    fun done(s: State) = value(s) >= goal
}

enum class Group(val title: L) {
    TRIPS(L("Outings", "Вылазки")), FINDS(L("Finds", "Находки")), NIGHTS(L("Nights", "Ночи")),
    CARE(L("Care", "Забота")), MAKING(L("Workshop", "Мастерская")), DAYS(L("Days together", "Дни вместе")),
}

/** Days in a row with care, counting back from today (or from yesterday, if today is still empty). */
object Streak {
    fun current(s: State, today: Long): Int {
        var d = if (today in s.careDays) today else today - 1
        var n = 0
        while (d in s.careDays) { n++; d-- }
        return n
    }

    fun best(s: State): Int {
        var best = 0; var run = 0; var prev = Long.MIN_VALUE
        for (d in s.careDays.sorted()) { run = if (d == prev + 1) run + 1 else 1; best = maxOf(best, run); prev = d }
        return best
    }
}

object Feats {
    private fun f(key: String, g: Group, en: String, ru: String, nEn: String, nRu: String, goal: Int, v: (State) -> Int) = Feat(key, g, L(en, ru), L(nEn, nRu), goal, v)

    private fun minutes(s: State) = s.trips.sumOf { it.minutes }
    private fun fullTrips(s: State) = s.trips.count { !it.cut }
    private fun good(s: State) = s.nights.count { it.grade == Grade.GOOD }
    private fun goodRun(s: State): Int {
        var best = 0; var run = 0
        for (n in s.nights.sortedBy { it.at }) { run = if (n.grade == Grade.GOOD) run + 1 else 0; best = maxOf(best, run) }
        return best
    }
    private fun cleanRun(s: State): Int {
        var best = 0; var run = 0
        for (t in s.trips.sortedBy { it.at }) { run = if (!t.cut) run + 1 else 0; best = maxOf(best, run) }
        return best
    }
    private fun wonders(s: State) = s.seen.keys.count { Catalog.find(it)?.rare == 3 }
    private fun builtOf(s: State, k: Kind) = Catalog.builds.count { it.kind == k && it.key in s.built }
    private fun allOf(k: Kind) = Catalog.builds.count { it.kind == k }

    val all: List<Feat> = buildList {
        // Outings.
        for ((n, en, ru) in listOf(Triple(1, "First steps", "Первые шаги"), Triple(5, "Getting around", "Освоился"), Triple(10, "Regular", "Завсегдатай"),
            Triple(25, "Old hand", "Бывалый"), Triple(50, "Pathfinder", "Следопыт"), Triple(100, "Legend of the board", "Легенда платы")))
            add(f("trips$n", Group.TRIPS, en, ru, "Come home from $n outings.", "Вылазок за всё время: $n.", n) { it.trips.size })
        for ((h, en, ru) in listOf(Triple(1, "An hour out", "Час в пути"), Triple(5, "Five hours", "Пять часов"), Triple(10, "Ten hours", "Десять часов"),
            Triple(24, "A day and a night", "Сутки на ногах"), Triple(50, "Fifty hours", "Полсотни часов")))
            add(f("hours$h", Group.TRIPS, en, ru, "$h hours on outings in all.", "Всего $h ч в вылазках.", h) { minutes(it) / 60 })
        for (d in District.entries) add(f("visit_${d.name.lowercase()}", Group.TRIPS, "Been to: ${d.name_.en}", "Побывал: ${d.name_.ru}",
            "Go on an outing there.", "Сходить туда на вылазку.", 1) { s -> s.trips.count { it.district == d } })
        add(f("long90", Group.TRIPS, "The long way", "Дальний путь", "An hour and a half without turning back.", "Вылазка на полтора часа до самого конца.", 1) { s -> s.trips.count { !it.cut && it.minutes >= 90 } })
        add(f("clean10", Group.TRIPS, "Phone down", "Телефон в сторону", "Ten outings in a row all the way.", "Десять вылазок подряд до конца.", 10) { cleanRun(it) })
        add(f("full25", Group.TRIPS, "Never halfway", "Ни шагу назад", "25 outings all the way.", "25 вылазок до конца.", 25) { fullTrips(it) })

        // Finds.
        for ((n, en, ru) in listOf(Triple(10, "Collector", "Собиратель"), Triple(25, "Magpie", "Сорока"), Triple(40, "Curator", "Хранитель"), Triple(Catalog.finds.size, "Everything there is", "Всё на свете")))
            add(f("finds$n", Group.FINDS, en, ru, "$n kinds of finds.", "Видов находок: $n.", n) { it.seen.size })
        for (d in District.entries) add(f("all_${d.name.lowercase()}", Group.FINDS, "Swept: ${d.name_.en}", "Обшарил: ${d.name_.ru}",
            "Every find from there.", "Все находки оттуда.", Catalog.of(d).size) { s -> Catalog.of(d).count { it.key in s.seen } })
        add(f("wonder1", Group.FINDS, "A wonder", "Диковина", "Find a wonder.", "Найти диковину.", 1) { wonders(it) })
        add(f("wonder5", Group.FINDS, "Cabinet of wonders", "Кунсткамера", "Five wonders.", "Пять диковин.", 5) { wonders(it) })

        // Nights.
        add(f("night1", Group.NIGHTS, "Slept well", "Выспался", "A good night.", "Хорошая ночь.", 1) { good(it) })
        add(f("night7", Group.NIGHTS, "A week of sleep", "Неделя сна", "Seven good nights.", "Семь хороших ночей.", 7) { good(it) })
        add(f("night30", Group.NIGHTS, "Master of sleep", "Мастер сна", "Thirty good nights.", "Тридцать хороших ночей.", 30) { good(it) })
        add(f("nightrun3", Group.NIGHTS, "Three in a row", "Три подряд", "Three good nights in a row.", "Три хорошие ночи подряд.", 3) { goodRun(it) })
        add(f("nightrun7", Group.NIGHTS, "Seven in a row", "Семь подряд", "Seven good nights in a row.", "Семь хороших ночей подряд.", 7) { goodRun(it) })

        // Care.
        for ((n, en, ru) in listOf(Triple(50, "Stroked", "Поглаженный"), Triple(300, "Cuddled", "Затисканный"), Triple(1000, "Thousand strokes", "Тысяча поглаживаний")))
            add(f("pets$n", Group.CARE, en, ru, "Stroke him $n times.", "Погладить $n раз.", n) { it.pets })
        for ((n, en, ru) in listOf(Triple(1, "First dish", "Первое блюдо"), Triple(10, "Home cooking", "Домашняя кухня"), Triple(30, "Chef", "Шеф")))
            add(f("dishes$n", Group.CARE, en, ru, "Cook $n dishes.", "Приготовить блюд: $n.", n) { it.dishes })
        for (d in Catalog.dishes) add(f("dish_${d.key}", Group.CARE, "Cooked: ${d.name.en}", "Блюдо: ${d.name.ru}", "Cook it once.", "Приготовить хоть раз.", 1) { it.cooked[d.key] ?: 0 })
        for ((n, en, ru) in listOf(Triple(1, "Rolled home", "Докатился"), Triple(10, "Maze runner", "Знаток лабиринта"), Triple(50, "Tilt master", "Мастер наклона")))
            add(f("maze$n", Group.CARE, en, ru, "Get through the maze $n times.", "Пройти лабиринт $n раз.", n) { it.gamesWon })

        for ((n, en, ru) in listOf(Triple(1, "Helping paw", "Помощник"), Triple(10, "Reliable", "Надёжный"), Triple(30, "Right paw", "Правая лапа")))
            add(f("asks$n", Group.CARE, en, ru, "Do $n of his weekly requests.", "Выполненных просьб: $n.", n) { it.requests.size })

        // Workshop.
        add(f("make1", Group.MAKING, "Handy", "Мастер на все лапы", "Make your first thing.", "Смастерить первую вещь.", 1) { it.built.size })
        add(f("tools", Group.MAKING, "Every road open", "Все дороги открыты", "Make every tool.", "Смастерить все инструменты.", allOf(Kind.TOOL)) { builtOf(it, Kind.TOOL) })
        add(f("home5", Group.MAKING, "Making it home", "Обжился", "Five things for the nook.", "Пять вещей для домика.", 5) { builtOf(it, Kind.HOME) })
        add(f("homeall", Group.MAKING, "Nothing missing", "Полная чаша", "Everything for the nook.", "Всё для домика.", allOf(Kind.HOME)) { builtOf(it, Kind.HOME) })
        add(f("wear5", Group.MAKING, "Wardrobe", "Гардеробчик", "Five things to wear.", "Пять вещей из одёжки.", 5) { builtOf(it, Kind.WEAR) })
        add(f("wearall", Group.MAKING, "Dandy", "Модник", "Every thing to wear.", "Вся одёжка.", allOf(Kind.WEAR)) { builtOf(it, Kind.WEAR) })

        // Days together.
        for ((n, en, ru) in listOf(Triple(3, "Three days running", "Три дня подряд"), Triple(7, "A week running", "Неделя подряд"), Triple(14, "Two weeks running", "Две недели подряд"),
            Triple(30, "A month running", "Месяц подряд"), Triple(100, "A hundred days", "Сто дней подряд")))
            add(f("streak$n", Group.DAYS, en, ru, "Look after him $n days in a row.", "Заботиться $n дней подряд.", n) { Streak.best(it) })
        add(f("morning1", Group.DAYS, "Good morning", "Доброе утро", "Say good morning.", "Пожелать доброго утра.", 1) { it.mornings })
        add(f("morning30", Group.DAYS, "Early bird", "Жаворонок", "Thirty good mornings.", "Тридцать добрых утр.", 30) { it.mornings })
        for (r in Rank.entries.drop(1)) add(f("rank_${r.name.lowercase()}", Group.DAYS, "Rank: ${r.title.en}", "Звание: ${r.title.ru}", "Reach the rank.", "Дорасти до звания.", r.from) { Rank.points(it) })
    }

    /** Achievements reached now that the household has not been told about yet. */
    fun fresh(s: State): List<Feat> = all.filter { it.key !in s.feats && it.done(s) }
}
