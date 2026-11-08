package app.rustle.domain

import app.rustle.data.DAY
import app.rustle.data.MIN
import app.rustle.data.State
import kotlin.random.Random

/**
 * The weekly spring-clean (#1): a week after the last one (or after moving in) he asks to wipe the glass.
 * It takes a cloth sewn in the workshop from threads; the cloth wears out in one clean. The reward is a
 * cheerful mood and something swept out from a corner.
 */
object SpringClean {
    /** A cloth: three threads of any colour, sewn again and again, unlike the things in [Catalog.builds]. */
    val cloth = L("Cloth", "Тряпочка")
    val clothNote = L("For the weekly spring-clean. Wears through in one.", "Для генеральной уборки раз в неделю. Хватает на одну.")
    val clothCost: Map<Shape, Int> = mapOf(Shape.THREAD to 3)
    /** No point in a drawer full of rags: threads are wanted for other things too. */
    const val MAX_CLOTHS = 3
    const val EVERY = 7 * DAY
    const val MOOD = 20

    /** When the last clean was, or the day he moved in. */
    fun since(s: State, now: Long) = if (s.cleanAt > 0) s.cleanAt else World.started(s, now)

    fun due(s: State, now: Long) = s.onboarded && now - since(s, now) >= EVERY

    fun canSew(s: State) = s.cloths < MAX_CLOTHS && Catalog.canPay(s.inv, clothCost)

    /** What turns up under the dust: a plain thing from an open district, the same for the same day. */
    fun find(s: State, day: Long): Find {
        val pool = World.open(s).flatMap { Catalog.of(it) }.filter { !it.shape.food && it.rare <= 2 }
        return pool.random(Random(day * 31 + 7))
    }
}

/**
 * A trip into the Net (#1), the light kind: on Wi-Fi he slips down the wires for [MINUTES] minutes, once a
 * day, and comes back with a piece of news from the world. Nothing is fetched; the news is his own.
 */
object Net {
    const val MINUTES = 10
    const val LENGTH = MINUTES * MIN

    fun inNet(s: State, now: Long) = s.netAt > 0 && now < s.netAt + LENGTH
    fun back(s: State, now: Long) = s.netAt > 0 && now >= s.netAt + LENGTH

    /** Whether he may go now: Wi-Fi, not out, not asleep, not been today. */
    fun canGo(s: State, now: Long, wifi: Boolean): Boolean {
        val out = s.trip?.let { it.ended == 0L && !it.over(now) } == true
        val asleep = s.night?.let { it.ended == 0L } == true
        return wifi && s.onboarded && s.netAt == 0L && s.netDay != World.dayOf(now) && !out && !asleep
    }

    /** The [n]-th piece of news for this household: a shuffled round, so none repeats until all are told. */
    fun line(s: State, n: Int): Int {
        val round = n / news.size
        val order = news.indices.shuffled(Random(s.started / DAY * 13 + round))
        return order[n % news.size]
    }

    private fun l(en: String, ru: String) = L(en, ru)

    val news: List<L> = listOf(
        l("I saw a thousand cats. Every one was sitting in a box. Now I want a box too.", "Видел тысячу котов. Каждый сидел в коробке. Теперь и мне хочется коробку."),
        l("Two people argued about one comma for three hours. The comma kept quiet the whole time.", "Двое три часа спорили из-за одной запятой. Запятая всё это время молчала."),
        l("The forecast says there will be weather tomorrow. Reliable people.", "Прогноз обещает на завтра погоду. Надёжные люди."),
        l("Someone baked bread and showed it to everyone. You could tell it was warm, even through the screen.", "Кто-то испёк хлеб и показал всем. Даже через экран видно, что тёплый."),
        l("I found a page nobody has touched since 2003. Quiet up there, like an attic.", "Нашёл страничку, которую не трогали с 2003 года. Тихо там, как на чердаке."),
        l("A man taught a crow to say «hello». The crow taught him to say «caw».", "Один человек научил ворону говорить «привет». А она его – говорить «кар»."),
        l("Everyone there is busy pressing little hearts. I pressed one too. For the bread.", "Там все заняты: жмут на сердечки. Я тоже нажал одно. Хлебу."),
        l("I watched a dog who didn't want a bath. I understand him completely.", "Видел пса, который не хотел купаться. Я его полностью понимаю."),
        l("Somewhere far away it's already tomorrow. They say it's fine.", "Где-то далеко уже наступило завтра. Говорят, ничего страшного."),
        l("I read the comments. I shouldn't have read the comments.", "Почитал комментарии. Зря я почитал комментарии."),
        l("There's a whole website about buttons. That's where I stayed longest.", "Есть целый сайт про пуговицы. Там я задержался дольше всего."),
        l("Someone found a glove in the park and posted a photo, so its owner could find it. People are kind.", "Кто-то нашёл в парке перчатку и выложил фото, чтобы хозяин отыскался. Хорошие всё-таки люди."),
        l("The Net is full of recipes. Not one of them is for a single raisin.", "В Сети тьма рецептов. И ни одного – из одной изюминки."),
        l("A grandmother learned to send stickers today. She sent forty.", "Одна бабушка сегодня научилась отправлять стикеры. Отправила сорок."),
        l("Everyone is arguing whether a hot dog is a sandwich. I kept out of it.", "Все спорят, бутерброд ли хот-дог. Я не стал вмешиваться."),
        l("The Wi-Fi was strong today. I floated along it like on a raft.", "Вай-фай сегодня крепкий. Я плыл по нему, как на плоту."),
        l("I tried to download more memory. It didn't work, but the button was pretty.", "Пробовал скачать ещё памяти. Не вышло, зато кнопка была красивая."),
        l("Someone is learning the violin and records every day. Day 212 is much better than day one.", "Кто-то учится играть на скрипке и каждый день записывается. На двести двенадцатый день уже гораздо лучше, чем на первый."),
        l("I watched a ten-hour video of a fireplace. Our stove is better.", "Посмотрел десятичасовое видео с камином. Наша печка лучше."),
        l("There are cats who keep people. The people seem happy.", "Бывают коты, у которых есть люди. Люди вроде довольны."),
        l("A tortoise at the zoo turned a hundred and four. She got a cake made of lettuce.", "У черепахи в зоопарке день рождения – сто четыре года. Ей подарили торт из салата."),
        l("In one town it's snowing in May. Everyone is taking pictures of lilac in the snow.", "В одном городе в мае выпал снег. Все фотографируют сирень в сугробах."),
        l("Someone wrote a very long letter and ended it with «sorry it's so long». I liked that.", "Кто-то написал очень длинное письмо, а в конце: «Простите, что так длинно». Мне понравилось."),
        l("A robot vacuum got stuck under a sofa, and everyone was cheering for it.", "Робот-пылесос застрял под диваном, и все за него болели."),
        l("I looked at the sea through a webcam. It was just getting on with its business.", "Посмотрел на море через веб-камеру. Оно просто занималось своими делами."),
        l("A whole city is choosing a name for the zoo's new bear cub. It's tense.", "Целый город выбирает имя медвежонку из зоопарка. Обстановка напряжённая."),
        l("Everyone writes «just one more video and then bed». I've seen how that ends.", "Все пишут: «Ещё одно видео – и спать». Я видел, чем это кончается."),
        l("Lots of people asked how to boil an egg today. Nobody asked me.", "Сегодня многие спрашивали, как сварить яйцо. Меня никто не спросил."),
        l("There's a duck who goes shopping with her owner. In little boots.", "Есть утка, которая ходит с хозяйкой в магазин. В ботиночках."),
        l("I found a map of the whole world. The Port isn't on it. Strange.", "Нашёл карту всего мира. Разъёма на ней нет. Странно."),
        l("Someone grew a pumpkin the size of a sofa. They're proud, and rightly so.", "Кто-то вырастил тыкву размером с диван. Гордится, и правильно."),
        l("I got lost on a forum of clockmakers. Time flew.", "Заблудился на форуме часовщиков. Время пролетело незаметно."),
        l("Everyone posted the moon tonight. On every phone it came out tiny.", "Сегодня все выкладывают луну. И у всех она получилась крошечной."),
        l("Four hundred people sang in a choir over video. Out of time, but very beautiful.", "Четыреста человек спели хором по видеосвязи. Вразнобой, но очень красиво."),
        l("I saw a rug cleaned «before» and «after». I want the same for our rug.", "Видел, как чистят ковёр: «до» и «после». Хочу так же с нашим ковриком."),
        l("A cat walked across a keyboard and sent «ffffffff» to someone's boss. The boss understood.", "Кот прошёлся по клавиатуре и отправил начальнику «ффффффф». Начальник всё понял."),
        l("Someone is building a ship in a bottle, live. I watched for an hour; he glued one rope.", "Кто-то в прямом эфире собирает кораблик в бутылке. Я смотрел час – он приклеил одну верёвочку."),
        l("They're arguing about how to eat watermelon: with a spoon or in slices. I'd just eat it.", "Спорят, как правильно есть арбуз: ложкой или ломтями. Я бы просто съел."),
        l("I saw a hedgehog eating an apple. The crunch was perfect.", "Видел ёжика, который ест яблоко. Хрустит идеально."),
        l("Everyone keeps forgetting their passwords. I keep mine in a matchbox.", "Все забывают пароли. А я свой храню в спичечном коробке."),
        l("I learned that an octopus has three hearts. Imagine how much it can love.", "Узнал, что у осьминога три сердца. Представляешь, сколько он может любить?"),
        l("A little girl asked why the sky is blue. She got four thousand answers, all different.", "Одна девочка спросила, почему небо голубое. Получила четыре тысячи ответов, и все разные."),
        l("A man in Norway knitted a jumper for his hen. The hen is not impressed.", "Один норвежец связал курице свитер. Курица не впечатлена."),
        l("Some people photograph their breakfast every single day. It's their diary.", "Есть люди, которые каждый день фотографируют завтрак. Это у них вместо дневника."),
        l("I watched a sunset in a city where it's already evening. Here it's still light. Muddling, but lovely.", "Посмотрел закат в городе, где уже вечер. А у нас ещё светло. Путано, но красиво."),
        l("Someone found an old letter in a library book and is looking for whoever wrote it. The whole Net is helping.", "Кто-то нашёл старое письмо в библиотечной книге и ищет, кто его написал. Помогают всей Сетью."),
        l("The news was loud today. I covered my ears and came home.", "Новости сегодня громкие. Я прикрыл уши и пошёл домой."),
        l("A thousand people showed their windowsill plants. One cactus has been alive since 1987.", "Тысяча человек показали свои цветы на подоконниках. Один кактус живёт с 1987 года."),
        l("I saw a seal clapping its flippers. I clapped back.", "Видел тюленя, который хлопает ластами. Похлопал ему в ответ."),
        l("There's a site where you can listen to the rain in any city. I listened to Lisbon.", "Есть сайт, где можно послушать дождь в любом городе. Я послушал Лиссабон."),
        l("Someone mended a fifty-year-old radio. It played a song older than everyone in the comments.", "Кто-то починил радиоприёмник, которому полвека. Он заиграл песню, которая старше всех в комментариях."),
        l("A puppy saw snow for the first time. So did I, in a way, through the camera.", "Щенок впервые увидел снег. Я, в каком-то смысле, тоже – через камеру."),
        l("Everyone shares tips on how to sleep better. At three in the morning.", "Все делятся советами, как лучше высыпаться. В три часа ночи."),
        l("It's mushroom season somewhere. People show off baskets; one man showed a single mushroom, just as proudly.", "Где-то пошли грибы. Хвастаются корзинками, а кто-то – одним подосиновиком, но так же гордо."),
        l("A pigeon got on a train and rode three stops. Nobody asked for its ticket.", "Голубь зашёл в электричку и проехал три остановки. Билет у него никто не спросил."),
        l("«Typing…», «typing…», and then nothing. I worried along with everyone.", "«Печатает…», «печатает…» – и тишина. Я переживал вместе со всеми."),
        l("I found a pie recipe with forty-one ingredients. Ours has two and tastes better.", "Нашёл рецепт пирога из сорока одного продукта. В нашем два, и он вкуснее."),
        l("Schoolchildren sent a balloon into the stratosphere with a toy hare on board. The hare came back a hero.", "Школьники запустили в стратосферу шарик с плюшевым зайцем. Заяц вернулся героем."),
        l("Someone wrote a song about a sock lost in the washing machine. Sad and very catchy.", "Кто-то сочинил песню про носок, пропавший в стиральной машине. Грустная и очень прилипчивая."),
        l("An old lady feeds a fox every evening. The fox turns up to the minute.", "Одна старушка каждый вечер кормит лису. Лиса приходит минута в минуту."),
        l("The Net is huge and noisy. My favourite part is coming home.", "Сеть огромная и шумная. Больше всего мне в ней нравится возвращаться домой."),
    )
}
