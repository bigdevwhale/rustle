# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.3.0] - 2026-11-13

Fixes and wishes from players (#2–#7).

- Outings (#2): he leaves as soon as the phone is locked or put away, not only when it is turned face down, and the face-down check is less fussy. While he is out and the screen is on, home shows how many minutes are left before he turns back, and he lets three minutes pass to put the phone down instead of one.
- Nights (#7): he falls asleep once the phone has lain dark for a quarter of an hour, so scrolling in bed after "to bed" no longer spoils the night; the night counts from then. A good night is five and a half hours with the screen on for ten minutes at most, an ordinary one four hours and half an hour.
- The kitchen (#3): a dish is eaten at once or put by in the pantry, and dishes in the pantry are given from the bowl later.
- The widget (#4) sends him where and for as long as is picked on the outings screen.
- He no longer looks cut in half as he breathes and walks (#5): he is dressed first and then moved as one picture, so hats and scarves keep in step with him, and breathing lifts him by a pixel below the scarf instead of stretching the sprite. The foxling also sleeps curled up anew, with one face.
- The scarf and the bell's ribbon (#6) go round the neck only, not across a tail or wings.
- Less scrolling, everywhere:
  - Home fits on one screen: this week's requests, the spring-clean, the Net and what to make next are small equal plates in one row under the actions, each opening its own sheet.
  - Outings: all five places in view at once as even rows (picture, name, how much is found or what to make first), the lengths as six equal buttons (15 to 90 and ∞), the log a sheet of its own; the place, the length, the time he'll be back and the button stay at the bottom. Nothing to swipe sideways, nothing cut off.
  - The workshop's cards are half as tall, the button beside the name; its four sections sit two by two, each with how many things are ready on it.
  - The tabs show the minutes left while he is out and how many things the workshop can make; the gauges say "hungry" and "glum" in words; the shelf reads out each find to a screen reader.
- Stroking him takes a tap on him, not anywhere in the nook.
- New pictures: all 62 finds, the icons, the five outing places and the nook's walls and floor are drawn with image generation through Codex (tools/gen.py) and shrunk into sprites by tools/art.py.

<!-- tg
Шуршик уходит гулять, как только заблокируешь телефон, а дома видно, сколько ещё можно смотреть в экран, пока он не повернул назад.
Ночь теперь начинается, когда отложишь телефон: полистать ленту перед сном больше не портит ночь.
На кухне можно готовить про запас в кладовку, а виджет отправляет туда и на столько, что выбрано в приложении.
Новые картинки: все находки, иконки, места для вылазок и стены домика нарисованы заново, а Шуршик больше не «разрезается» при дыхании, и шапка с шарфом двигаются вместе с ним.
-->

## [1.2.0] - 2026-11-11

- All eight kinds drawn anew, each with a shape of its own: the hedgehog in a coat of spines, the lop with long ears and a cotton tail, the moth with fern-like feelers and spotted wings, the foxling with a bushy white-tipped tail, the owlet with a facial disc and folded wings, the kit with a question-mark tail, the flitter on wide webbed wings. Each one also has its own way of curling up to sleep.
- Fur colours, a colour from a photo, patterns, clothes and every mood still fit every kind: he blinks, beams, gets dizzy and frowns when hungry on the new faces.
- The sheets were made with image generation through Codex and are cut into sprites by tools/cut.py.

<!-- tg
Все восемь зверьков нарисованы заново: у ёжика колючая шубка, у лисёнка пушистый хвост, у совёнка мягкие крылья, у мотылька усики-папоротники.
И у каждого свой сон: кто-то сворачивается клубком, кто-то укрывается крыльями.
Цвет шерсти, узоры, одёжки и настроения остались на месте и подходят всем.
-->

## [1.1.0] - 2026-11-08

Ideas from a player (#1).

- A weekly spring-clean: a week after the last one the glass gets dusty and he asks to wipe it. Sew a cloth in the workshop from three threads of any colour; he rubs the screen from the inside until it sparkles, the cloth wears through, and he is in a fine mood and sweeps something out of a corner.
- Trips into the Net: when the phone is on Wi-Fi, once a day, he can pop into the Net for ten minutes and come back with a piece of news of the world – 61 of them, from cat videos to a quarrel over a comma. Nothing is downloaded: the news is his own.
- Livelier letters: sentences are set in the round, hand-drawn Balsamiq Sans instead of the phone's plain face, and his own words in Neucha, like a pencilled note.
- Settings › StillCraft: a «Support Rustle» line that opens the StillCraft support page.

<!-- tg
Раз в неделю Шуршик просит устроить генеральную уборку: тряпочку шьют в мастерской из трёх ниток, а он трёт стекло изнутри, пока не заблестит.
Когда телефон в вай-фае, Шуршик раз в день может сбегать в Сеть и вернуться с новостями: про котов, погоду и спор из-за запятой.
Новые шрифты: текст – округлый, будто от руки, а реплики Шуршика – как записка карандашом.
В настройках появилась строчка «Поддержать Шуршика» – на дрова для печки и новые вылазки.
-->

## [1.0.0]

The first version.

- A creature living inside the phone: the battery is its stove, the camera its window; it reacts to charging, a low battery, darkness, sunlight, shaking, heat and full storage.
- Outings with the phone face down to five places, ended early by long screen time; free walks that end when the phone is picked up; a live notification.
- Nights on charge, graded by how dark the screen stayed, with a find from a dream.
- 62 finds, a workshop of 43 things (tools, furniture in five places, 21 clothes with try-on) and a kitchen of four dishes.
- Eight kinds of creature, eight colours or a colour from a photo, six patterns, character traits that grow from play.
- Mood and hunger that change an outing's haul, weekly requests, 67 achievements, ranks, a run of days, a morning present.
- A tilt maze, a home-screen widget, dark and light themes with five background colours, optional real weather in the window.
- First-visit hints on every screen. English and Russian.

<!-- tg
Шуршик — зверёк, который живёт внутри телефона: батарея у него печка, камера — окно на улицу.
Переверните телефон экраном вниз, и он уйдёт гулять по закоулкам и вернётся с находками.
Из находок мастерят инструменты, мебель и одёжку, а ночью он спит у печки, пока телефон заряжается.
-->
