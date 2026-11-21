package fr.kiki.pixoo

/** Une image de 16 × 16 pixels, comme l'écran du cadre. Couleur = 0xRRGGBB. (0, 0) = en haut à gauche.
 *  Kotlin pur (aucun Android) : se dessine et se teste partout. */
class Image16(fond: Int = NOIR) {
    val pixels = IntArray(TAILLE * TAILLE) { fond }

    operator fun get(x: Int, y: Int) = pixels[y * TAILLE + x]

    /** Un pixel ; hors de l'écran = ignoré (on peut dessiner à cheval sur un bord sans se soucier). */
    operator fun set(x: Int, y: Int, c: Int) {
        if (x in 0 until TAILLE && y in 0 until TAILLE) pixels[y * TAILLE + x] = c and 0xFFFFFF
    }

    fun remplir(c: Int) = apply { pixels.fill(c and 0xFFFFFF) }

    /** Rectangle plein : coin (x, y), largeur l, hauteur h. */
    fun rect(x: Int, y: Int, l: Int, h: Int, c: Int) = apply {
        for (j in y until y + h) for (i in x until x + l) this[i, j] = c
    }

    /** Écrit un texte avec la petite police (chiffres 3 × 5, voir [Police]). Rend la largeur écrite. */
    fun ecrire(texte: String, x: Int, y: Int, c: Int, echelle: Int = 1): Int = Police.ecrire(this, texte, x, y, c, echelle)

    fun copie() = Image16().also { pixels.copyInto(it.pixels) }

    companion object {
        const val TAILLE = 16
        const val NOIR = 0x000000
        const val BLANC = 0xFFFFFF

        /** Image tirée de lignes de texte, une lettre par pixel ; `couleurs` dit quelle lettre donne quelle couleur
         *  (« . » ou une lettre absente = fond). Pratique pour dessiner à la main un petit motif. */
        fun dessin(lignes: List<String>, couleurs: Map<Char, Int>, fond: Int = NOIR): Image16 {
            val img = Image16(fond)
            lignes.forEachIndexed { y, ligne -> ligne.forEachIndexed { x, ch -> couleurs[ch]?.let { img[x, y] = it } } }
            return img
        }
    }
}
