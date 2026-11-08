package app.rustle.domain

/** What he is doing right now, in order of what matters most. */
enum class Pose { AWAY, SLEEP, DIZZY, COLD, HOT, HUNGRY, DARK, SUN, WARM, IDLE }

/** What he says at home and what he tells after an outing. */
object Lines {
    private fun l(en: String, ru: String) = L(en, ru)

    val say: Map<Pose, List<L>> = mapOf(
        Pose.SLEEP to listOf(l("Zzz… shh, the stove is warm.", "Хр-р… тш-ш, печка тёплая."), l("…three buttons and a raisin…", "…три пуговки и изюминка…")),
        Pose.DIZZY to listOf(l("Whoa-whoa-whoa! The walls are spinning!", "Ой-ой-ой! Стены кружатся!"), l("Is that an earthquake or are you dancing?", "Это землетрясение или ты танцуешь?")),
        Pose.COLD to listOf(l("Brr. The stove is almost out. Plug me in?", "Бр-р. Печка почти погасла. Подключишь зарядку?"), l("I've wrapped up in my blanket. Some charge would help.", "Завернулся в плед. Мне бы немного заряда.")),
        Pose.HOT to listOf(l("Phew, it's hot in here. Put me down for a bit?", "Уф, жарко. Дай телефону остыть?"), l("Even the solder is sweating.", "Даже пайка вспотела.")),
        Pose.HUNGRY to listOf(l("My tummy's rustling louder than I am.", "Живот шуршит громче меня."), l("Is there a crumb in the pantry?", "В кладовке не найдётся крошки?")),
        Pose.DARK to listOf(l("It's dark. I lit a candle.", "Темно. Я зажёг свечку."), l("Shh. In the dark you can hear the battery hum.", "Тш-ш. В темноте слышно, как гудит батарея.")),
        Pose.SUN to listOf(l("Sun through the camera! I'm sunbathing.", "Солнце в камере! Загораю."), l("Warm on my nose. Stay like this.", "Нос греется. Постой так.")),
        Pose.WARM to listOf(l("The stove is lit. Lovely.", "Печка топится. Хорошо-то как."), l("Charging is my favourite weather.", "Зарядка — моя любимая погода.")),
        Pose.IDLE to listOf(
            l("Put me face down and I'll go for a walk.", "Положи меня экраном вниз — схожу погулять."),
            l("I heard something rattle in the Cellar.", "В Погребе что-то звякнуло, я слышал."),
            l("I'm sorting buttons. Don't mind me.", "Перебираю пуговицы. Не обращай внимания."),
            l("You've been looking at the screen a long time. Want me to go out?", "Ты давно смотришь в экран. Мне сходить погулять?"),
            l("I like it best when the screen is dark and you have better things to do.", "Мне больше всего нравится, когда экран погас, а у тебя дела поинтереснее."),
        ),
    )

    /** What he says when a week has gone by since the last spring-clean (#1). */
    val dusty = listOf(
        l("There's dust on the glass. I can hardly see you.", "На стекле пыль. Я тебя еле вижу."),
        l("Time for a spring-clean, don't you think? Have we got a cloth?", "Пора бы устроить генеральную уборку. Тряпочка у нас есть?"),
    )

    /** What he says about the weather outside the window. */
    val weather: Map<Wx, List<L>> = mapOf(
        Wx.RAIN to listOf(l("Rain on the window. Good to be home.", "Дождь по окну. Хорошо, что я дома."), l("Listen to it drumming on the camera.", "Слышишь, как стучит по камере?")),
        Wx.DRIZZLE to listOf(l("Drizzle. The roofs across the street are shiny.", "Морось. Крыши напротив блестят.")),
        Wx.STORM to listOf(l("Thunder! I'm sitting closer to the stove.", "Гром! Я сяду поближе к печке."), l("What a flash that was!", "Ну и вспышка была!")),
        Wx.SNOW to listOf(l("Snow! The roofs have white caps.", "Снег! У крыш белые шапки."), l("Snowflakes on the glass. Each one different.", "Снежинки на стекле. И все разные.")),
        Wx.FOG to listOf(l("Fog. I can't see the houses across the street.", "Туман. Домов напротив не видно.")),
        Wx.OVERCAST to listOf(l("Grey outside. A good day for the stove.", "Серо за окном. Самое время для печки.")),
        Wx.CLEAR to listOf(l("Clear sky in the window.", "В окне чистое небо.")),
        Wx.CLOUDY to listOf(l("The clouds are in a hurry today.", "Облака сегодня куда-то спешат.")),
    )

    /** Notes he brings back, one per district. */
    val trip: Map<District, List<L>> = mapOf(
        District.PORT to listOf(
            l("There was a draught from the port. I held on to a pin and waited.", "Из разъёма дуло. Я держался за булавку и пережидал."),
            l("Found a cable that was plugged in years ago. It remembers.", "Нашёл кабель, который когда-то втыкали. Он всё помнит."),
            l("Someone dropped crumbs in the pocket again. I'm not complaining.", "Кто-то опять накрошил в кармане. Я не жалуюсь."),
            l("Sat by the port and listened to the street.", "Посидел у разъёма, послушал улицу."),
        ),
        District.CELLAR to listOf(
            l("It's warm under the battery. I nearly fell asleep.", "Под батареей тепло. Чуть не уснул."),
            l("The cells were humming a song. I hummed along.", "Ячейки гудели какую-то песню. Я подпевал."),
            l("Raisins dry best next to the hot side.", "Изюм лучше всего сохнет у горячей стенки."),
            l("I counted coins in the dark. There were more than yesterday.", "Считал в темноте монетки. Их больше, чем вчера."),
        ),
        District.BRIDGE to listOf(
            l("I walked the ribbon bridge without looking down. Almost.", "Прошёл по шлейфу и не смотрел вниз. Почти."),
            l("The wind between the boards brought a feather.", "Ветер между платами принёс пёрышко."),
            l("Tied a knot on the railing so I can find my way back.", "Завязал узелок на перилах, чтобы найти дорогу."),
            l("A bead rolled all the way across. I caught it at the edge.", "Бусина катилась через весь мост. Поймал у самого края."),
        ),
        District.GORGE to listOf(
            l("Someone called and the whole gorge rang. I kept my earmuffs on.", "Кто-то позвонил, и ущелье загудело. Я не снимал наушники."),
            l("Echoes keep the sequins shaken loose.", "От эха блёстки сами осыпаются."),
            l("I shouted «hello» and the gorge answered three times.", "Крикнул «эй», и ущелье ответило три раза."),
            l("Found an old ringtone lying in the corner.", "В углу валялась старая мелодия звонка."),
        ),
        District.DOME to listOf(
            l("Lay under the glass and watched clouds pass.", "Лежал под стеклом и смотрел на облака."),
            l("A star fell right on the lens. Well, nearly.", "Звезда упала прямо на линзу. Ну, почти."),
            l("Up here you can see the whole room. There is a book on your table.", "Отсюда видно всю комнату. На столе у тебя книжка."),
            l("The moon in the camera is the size of a button.", "Луна в камере размером с пуговицу."),
        ),
    )

    val cut = listOf(
        l("I heard you pick up the phone and came back halfway.", "Услышал, что ты взял телефон, и вернулся с полпути."),
        l("The screen lit up and I turned round. Next time, all the way?", "Экран зажёгся, и я повернул назад. В следующий раз дойдём?"),
    )

    val doorstep = l("I only got as far as the doorstep. Next time give me a little longer.", "Я дошёл только до порога. В следующий раз дай мне побольше времени.")

    val night: Map<Grade, List<L>> = mapOf(
        Grade.GOOD to listOf(
            l("Slept like a log by the stove. I dreamt of a button the size of a moon.", "Спал у печки без задних лап. Снилась пуговица размером с луну."),
            l("Not a single light all night. I'm full of sleep.", "За всю ночь ни огонька. Выспался на неделю вперёд."),
        ),
        Grade.OK to listOf(
            l("Slept all right. Woke up once when the screen lit up.", "Спал неплохо. Один раз проснулся, когда загорелся экран."),
            l("The night was short but warm.", "Ночь была короткая, но тёплая."),
        ),
        Grade.BAD to listOf(
            l("The screen kept lighting up. I counted the flashes instead of sheep.", "Экран всё загорался. Я считал вспышки вместо овец."),
            l("Didn't sleep much. Was something on your mind?", "Почти не спал. Тебе что-то не давало покоя?"),
        ),
    )
}
