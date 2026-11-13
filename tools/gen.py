"""
Draws the app's pictures with image generation through Codex: the finds, the icons, the outing places and
the nook's wall and floor. Each job is one `codex exec` call that makes one image; the PNG lands in art/.
tools/art.py then shrinks them into the sprites the app reads.

    python tools/gen.py [group|name ...]   # e.g. `places`, `items`, `icons`, `room`, `honey`
    python tools/gen.py --force ...         # draw again what is already there

Jobs run six at a time; an image already in art/ is skipped unless --force.
"""
import concurrent.futures as cf
import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")
REF = os.path.join(ART, "pets", "fox.png")
_JS = os.path.join(os.environ.get("APPDATA", ""), "npm", "node_modules", "@openai", "codex", "bin", "codex.js")
CODEX = ["node", _JS] if os.path.exists(_JS) else [shutil.which("codex") or "codex"]

STYLE = ("Chunky pixel art in a cosy 16-bit game style, like the attached reference: crisp thick dark outline, "
         "soft cel shading with 3-4 tones per colour, warm rich colours, a little white glint on shiny parts.")

ITEM = (STYLE + "\n\nOne single object, an item icon for an inventory, centred, filling most of the square picture, "
        "seen from slightly above at a three-quarter angle. Big simple readable shapes: it will be shrunk to 24x24 "
        "pixels, so no tiny details and no thin lines. Transparent background. No shadow under it, no text, no "
        "frame, no grid, nothing else in the picture.\n\nThe object: {what}")

ICON = (STYLE + "\n\nOne single icon for a game's menu button, centred, filling most of the square picture, front view. "
        "Bold simple silhouette that still reads at 24x24 pixels: no tiny details, no thin lines. Transparent "
        "background. No shadow, no text, no frame, nothing else in the picture.\n\nThe icon: {what}")

PLACE = (STYLE + "\n\nA wide landscape pixel-art scene for a cosy game where a tiny creature lives inside a smartphone and "
         "goes on outings to places inside the phone. Aspect ratio about 16:7, full bleed, no border, no text, no "
         "creatures or people. Big simple shapes and strong contrast, still readable when shrunk to 96x42 pixels.\n\n"
         "The place: {what}")

# What a find is, by key: drawn from its name in the catalogue and its colour there.
FINDS = {
    "pin_brass": "a brass sewing pin with a round brass head, lying diagonally (#E0B050)",
    "pin_silver": "a silver sewing pin with a round head, lying diagonally (#C8CED6)",
    "button_red": "a round red sewing button with two holes (#D24A3A)",
    "button_shirt": "a small off-white shirt button with four holes, pearly sheen (#F2EEE2)",
    "button_coat": "a big dark brown coat button with a raised rim and four holes (#7A4A2A)",
    "screw_tiny": "a tiny steel screw with a cross head, lying on its side (#B8C0C8)",
    "screw_gold": "a gilt golden screw with a slotted head, lying on its side (#F0C050)",
    "thread_red": "a short loose length of red sewing thread curled into loops (#D24A3A)",
    "thread_blue": "a short loose length of blue sewing thread curled into loops (#4A72C8)",
    "led_red": "a red 5mm LED with two metal legs, glowing a little (#F05040)",
    "sugar": "a single sparkling white grain of sugar, a small glittering crystal (#FFFFFF)",
    "rusk": "a golden toasted rusk, a slice of crunchy dried bread (#D8A060)",
    "scrap_ticket": "a torn bit of an old yellowish tram ticket with a punched hole and printed digits (#E8D890)",
    "key_diary": "a small ornate golden key for a diary lock, with a heart-shaped bow (#E0B050)",
    "coin_copper": "a copper coin, warm orange-brown, with a simple embossed mark (#D08040)",
    "coin_foreign": "a silver foreign coin with a hole in the middle (#D8DCE0)",
    "raisin": "a single plump wrinkled dark purple raisin (#6A3050)",
    "honey": "a big glossy golden drop of honey (#F0A830)",
    "rusk_rye": "a dark brown rye rusk, a crunchy slice of dried rye bread (#9A6038)",
    "magnet": "a little red fridge magnet shaped like a strawberry (#D24A3A)",
    "magnet_shoe": "a blue horseshoe magnet with silver tips (#4A72C8)",
    "spring_coil": "a short steel coil spring, standing upright (#B8C0C8)",
    "led_amber": "an amber orange 5mm LED with two metal legs, glowing warmly (#F0A030)",
    "screw_black": "a black steel screw with a cross head, lying on its side (#505860)",
    "key_brass": "an old brass key with a round bow and simple teeth (#D8A848)",
    "gem_amber": "a translucent drop of amber glowing honey-orange (#F09A28)",
    "thread_gold": "a short loose length of shining gold thread curled into loops (#F0C050)",
    "thread_green": "a short loose length of green sewing thread curled into loops (#58A050)",
    "glass_bottle": "a smooth sea-worn chip of green bottle glass (#6AB88A)",
    "feather_sparrow": "a small brown sparrow feather with darker stripes (#9A7450)",
    "feather_jay": "a bright blue jay feather with black and white bars (#4A8AD8)",
    "bead_glass": "a round clear pale-cyan glass bead with a hole (#9AD8E0)",
    "bead_red": "a round shiny red bead with a hole (#D24A3A)",
    "bead_pearl": "a tiny round white pearl with a soft pink sheen (#F4F0E8)",
    "spring_long": "a long thin brass spring lying on its side (#D8B060)",
    "pin_safety": "a silver safety pin, closed (#D0D4DA)",
    "button_four": "a round green button with four holes (#58A050)",
    "seed_sunflower": "a single striped black and grey sunflower seed (#404040)",
    "scrap_letter": "the torn-off corner of a handwritten letter on cream paper, a few lines of ink (#F2EEE2)",
    "star_sequin": "a pink-magenta sparkly sequin shaped like a little star (#E070C0)",
    "star_gold": "a shiny gold paper star sticker (#F8D050)",
    "ring_tin": "a plain grey tin ring (#B8C0C8)",
    "ring_silver": "a polished silver ring with a tiny blue stone (#E8ECF0)",
    "bead_black": "a round glossy black bead with a hole (#303038)",
    "spring_bell": "a coiled golden doorbell spring with a little hammer (#E0B050)",
    "coin_token": "a brass metro token with a letter M cut through it (#C8A050)",
    "feather_owl": "a soft tawny owl feather with dark bars (#C8A878)",
    "glass_frost": "a chip of frosted milky-white glass, matte, pale blue edges (#C8D8E0)",
    "led_green": "a green 5mm LED with two metal legs, glowing (#60E070)",
    "sugar_cube": "a white sugar cube with sparkling grains (#FFFFFF)",
    "scrap_music": "a torn bit of old sheet music with a few black notes on a stave (#E8E0C8)",
    "gem_lens": "a chip of a glass camera lens, curved, with a blue-violet coated shine (#88C8F0)",
    "gem_violet": "a small glowing violet crystal crumb (#A070D8)",
    "star_fallen": "a small fallen star: a softly glowing pale yellow five-pointed star with a few sparkles (#FFF0A0)",
    "ring_gold": "a gold ring with a little round gem (#F8C840)",
    "sugar_star": "a star-shaped piece of pale yellow konpeito star sugar (#FFF4C0)",
    "feather_white": "a fluffy pure white feather (#F4F4F0)",
    "key_star": "a pale silver-blue key whose bow is a five-pointed star (#C8D8F8)",
    "bead_moon": "a round pale bead that looks like a tiny full moon with craters (#D8E0F0)",
    "led_white": "a white 5mm LED with two metal legs, glowing bright (#F0F4FF)",
    "seed_moon": "a pale silvery seed shaped like a crescent moon, faintly glowing (#B8C0E0)",
    "scrap_chart": "a torn piece of a dark navy star chart with little white stars joined by lines (#30406A)",
}

ICONS = {
    "home": "a tiny cosy house with a red roof, a round window glowing warm and a chimney",
    "pack": "a small leather explorer's satchel with a brass buckle and a strap",
    "chest": "a small wooden treasure chest with brass corners, lid closed",
    "tools": "a hammer and a screwdriver crossed",
    "bowl": "a red bowl of porridge with a wooden spoon",
    "maze": "a small square wooden tilt-maze board, green with copper walls and a silver ball",
    "moon": "a yellow crescent moon with a tiny star",
    "sun": "a round yellow sun with short rays",
    "heart": "a glossy red heart",
}

PLACES = {
    "port": "The Port: a big USB-C charging port opening in a brushed grey metal wall, warm daylight from the street "
            "pouring in through the slot, a little wooden walkway leading to it, a dropped brass pin on the floor, "
            "dust motes in the light.",
    "cellar": "The Battery Cellar: a warm dark cellar under a phone battery, rows of big battery cells like barrels with "
              "copper caps, a soft orange glow, raisins drying on a string across the ceiling, a lantern on a hook.",
    "bridge": "The Ribbon Bridge: a flat amber ribbon cable spanning a deep dark gap between two green circuit boards like "
              "a rope bridge, golden contact pads on its edges, tiny lights far below.",
    "gorge": "The Speaker Gorge: inside a phone speaker, a deep round chamber with a dark metal mesh wall, rings of "
             "sound drawn as soft pink waves rippling out, little yellow sparks of noise.",
    "dome": "The Observatory: under the round glass of the phone camera, a domed lens above showing the night sky with "
            "stars and a crescent moon, a little brass ladder and a telescope on the rim.",
}

ROOM = (STYLE + "\n\nA pixel-art background for a cosy game, portrait aspect about 6:7, full bleed, no border, no text, front "
        "view of an EMPTY little room inside a smartphone, with nothing standing in it: no furniture, no windows, no "
        "creatures. It is only the room's shell, drawn flat and straight-on so that furniture can be placed on it later.\n"
        "From top to bottom, in straight horizontal bands:\n"
        "- at the very top, a thin ceiling beam: an amber flat ribbon cable with fine copper lines, held by two black "
        "clips at its ends (about 6% of the height);\n"
        "- the back wall: a deep green circuit board, dark and calm, with faint copper tracks turning at 45 degrees, a "
        "few vias, faded white silkscreen marks and a few far-off muted chips and capacitors (down to about 68%);\n"
        "- a skirting band: a copper rail on top of a row of gold contact fingers, like the edge connector of a board "
        "(about 68% to 78%);\n"
        "- the floor: warm reddish-brown wooden planks in rows that widen towards the viewer, with grain, knots and "
        "little nail heads (78% to the bottom).\n"
        "Even, soft lighting. Calm, muted, not busy: it is a background behind brighter objects.")


# Pets drawn again in part: the new figure is pasted into the kind's sheet by tools/art.py.
PETS = {
    "fox_lie": (os.path.join(ART, "sheets", "fox.png"),
                "The attached sheet shows a fox kit sitting (left) and curled up asleep (right). Draw ONLY a new version of "
                "the sleeping one, in exactly the same pixel art style, colours and size as the sitting fox: the fox kit "
                "curled into a round ball asleep, seen from the front, its head resting on top of its front paws in the "
                "middle, one clear face with both eyes closed as two short curved lines, a small dark nose and pink blush, "
                "the two ears up with dark tips, and its big bushy orange tail with a white tip wrapped around the front of "
                "its body below the face. One single figure, symmetrical face, nothing doubled. Transparent background, no "
                "shadow, no text, nothing else in the picture."),
}


def jobs():
    out = []
    for k, (ref, w) in PETS.items():
        out.append(("pets_new", k, w, ref))
    for k, w in FINDS.items():
        out.append(("items", k, ITEM.format(what=w)))
    for k, w in ICONS.items():
        out.append(("icons", k, ICON.format(what=w)))
    for k, w in PLACES.items():
        out.append(("places", k, PLACE.format(what=w)))
    out.append(("room", "room", ROOM))
    return out


def run(group, name, prompt, ref=REF):
    dest = os.path.join(ART, group, name + ".png")
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    text = ("Use your image generation tool to create ONE image.\n\n" + prompt +
            "\n\nDo not save or copy the file anywhere and change no files. Reply with only the full path of the generated PNG.")
    for attempt in range(3):
        # The prompt goes in on stdin: a long many-line argument does not survive codex.cmd on Windows.
        try:
            r = subprocess.run(CODEX + ["exec", "-s", "workspace-write", "--skip-git-repo-check", "--ephemeral", "-i", ref, "-"],
                               cwd=ROOT, input=text, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=900)
        except subprocess.TimeoutExpired:
            continue
        paths = re.findall(r"[A-Za-z]:[\\/][^\s`'\"]*generated_images[\\/][^\s`'\"]+?\.png", r.stdout + r.stderr)
        paths = [p for p in paths if os.path.exists(p)]
        if paths:
            shutil.copyfile(paths[-1], dest)
            return f"{group}/{name} ok"
    return f"{group}/{name} FAILED"


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    force = "--force" in sys.argv
    todo = [j for j in jobs() if not args or j[0] in args or j[1] in args]
    if not force:
        todo = [j for j in todo if not os.path.exists(os.path.join(ART, j[0], j[1] + ".png"))]
    print(len(todo), "to draw", flush=True)
    with cf.ThreadPoolExecutor(6) as ex:
        for res in ex.map(lambda j: run(*j), todo):
            print(res, flush=True)


if __name__ == "__main__":
    main()
