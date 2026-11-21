package fr.kiki.pixoo

/** Petite police de 3 pixels de large sur 5 de haut (chiffres, lettres A-Z sans accents, quelques signes), avec 1 pixel d'écart.
 *  Sur 16 pixels, on loge par exemple « 40 » et « 15 » côte à côte. `echelle` = 2 double tout (6 × 10). */
object Police {
    const val LARGEUR = 3
    const val HAUTEUR = 5

    private val GLYPHES: Map<Char, List<String>> = mapOf(
        '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
        '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
        '2' to listOf("###", "..#", "###", "#..", "###"),
        '3' to listOf("###", "..#", ".##", "..#", "###"),
        '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
        '5' to listOf("###", "#..", "###", "..#", "###"),
        '6' to listOf("###", "#..", "###", "#.#", "###"),
        '7' to listOf("###", "..#", ".#.", ".#.", ".#."),
        '8' to listOf("###", "#.#", "###", "#.#", "###"),
        '9' to listOf("###", "#.#", "###", "..#", "###"),
        'A' to listOf(".#.", "#.#", "###", "#.#", "#.#"),
        'V' to listOf("#.#", "#.#", "#.#", "#.#", ".#."),
        'D' to listOf("##.", "#.#", "#.#", "#.#", "##."),
        'E' to listOf("###", "#..", "##.", "#..", "###"),
        'F' to listOf("###", "#..", "##.", "#..", "#.."),
        'I' to listOf("###", ".#.", ".#.", ".#.", "###"),
        'J' to listOf("..#", "..#", "..#", "#.#", "###"),
        'K' to listOf("#.#", "#.#", "##.", "#.#", "#.#"),
        'N' to listOf("##.", "#.#", "#.#", "#.#", "#.#"),
        'O' to listOf("###", "#.#", "#.#", "#.#", "###"),
        'P' to listOf("###", "#.#", "###", "#..", "#.."),
        'S' to listOf("###", "#..", "###", "..#", "###"),
        'T' to listOf("###", ".#.", ".#.", ".#.", ".#."),
        'U' to listOf("#.#", "#.#", "#.#", "#.#", "###"),
        'B' to listOf("##.", "#.#", "##.", "#.#", "##."),
        'C' to listOf("###", "#..", "#..", "#..", "###"),
        'G' to listOf("###", "#..", "#.#", "#.#", "###"),
        'H' to listOf("#.#", "#.#", "###", "#.#", "#.#"),
        'L' to listOf("#..", "#..", "#..", "#..", "###"),
        'M' to listOf("#.#", "###", "#.#", "#.#", "#.#"),
        'Q' to listOf("###", "#.#", "#.#", "###", "..#"),
        'R' to listOf("##.", "#.#", "##.", "#.#", "#.#"),
        'W' to listOf("#.#", "#.#", "###", "###", "#.#"),
        'X' to listOf("#.#", "#.#", ".#.", "#.#", "#.#"),
        'Y' to listOf("#.#", "#.#", ".#.", ".#.", ".#."),
        'Z' to listOf("###", "..#", ".#.", "#..", "###"),
        '-' to listOf("...", "...", "###", "...", "..."),
        ':' to listOf("...", ".#.", "...", ".#.", "..."),
        ' ' to listOf("...", "...", "...", "...", "..."),
    )

    fun connait(c: Char) = GLYPHES.containsKey(c.uppercaseChar())

    /** Largeur en pixels d'un texte (sans l'écart final). */
    fun largeur(texte: String, echelle: Int = 1): Int =
        if (texte.isEmpty()) 0 else (texte.length * (LARGEUR + 1) - 1) * echelle

    /** Écrit `texte` à partir du coin (x, y). Caractère inconnu = un blanc. Rend la largeur écrite. */
    fun ecrire(img: Image16, texte: String, x: Int, y: Int, c: Int, echelle: Int = 1): Int {
        var cx = x
        for (ch in texte) {
            val g = GLYPHES[ch.uppercaseChar()] ?: GLYPHES.getValue(' ')
            for (j in 0 until HAUTEUR) for (i in 0 until LARGEUR) {
                if (g[j][i] == '#') img.rect(cx + i * echelle, y + j * echelle, echelle, echelle, c)
            }
            cx += (LARGEUR + 1) * echelle
        }
        return largeur(texte, echelle)
    }
}
