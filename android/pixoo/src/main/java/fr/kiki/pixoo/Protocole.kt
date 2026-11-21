package fr.kiki.pixoo

/** Le langage du cadre Divoom Pixoo (16 × 16, Bluetooth classique), d'après pixoo-client de virtualabs
 *  (github.com/virtualabs/pixoo-client), réécrit ici en Kotlin. Kotlin pur : aucun Bluetooth, seulement des octets.
 *
 *  Un message = 0x01, taille (2 octets, petit bout d'abord), commande, données, somme de contrôle (2 octets), 0x02.
 *  Certains cadres plus récents veulent que les octets 0x01, 0x02, 0x03 du milieu soient « échappés »
 *  (0x03 puis l'octet + 3) : c'est l'option `echapper`, à essayer si rien ne s'affiche. */
object Protocole {
    const val LUMINOSITE = 0x74
    const val IMAGE = 0x44
    const val ANIMATION = 0x49

    /** Un message complet, prêt à partir. */
    fun message(cmd: Int, donnees: List<Int>, echapper: Boolean = false): ByteArray {
        val taille = donnees.size + 3
        val corps = listOf(taille and 0xFF, (taille shr 8) and 0xFF, cmd and 0xFF) + donnees.map { it and 0xFF }
        val somme = corps.sum() and 0xFFFF
        val milieu = corps + listOf(somme and 0xFF, (somme shr 8) and 0xFF)
        val sortie = ArrayList<Int>(milieu.size + 4)
        sortie.add(0x01)
        for (b in milieu) {
            if (echapper && b in 0x01..0x03) { sortie.add(0x03); sortie.add(b + 3) } else sortie.add(b)
        }
        sortie.add(0x02)
        return ByteArray(sortie.size) { sortie[it].toByte() }
    }

    /** Luminosité de 0 à 100. */
    fun luminosite(pct: Int, echapper: Boolean = false) = message(LUMINOSITE, listOf(pct.coerceIn(0, 100)), echapper)

    /** L'image codée comme le veut le cadre : palette des couleurs (dans l'ordre où on les rencontre, ligne par ligne)
     *  puis chaque pixel = son numéro dans la palette, sur juste assez de bits, rangés en commençant par les bits faibles. */
    class Codee(val couleurs: Int, val palette: List<Int>, val pixels: List<Int>)

    fun coder(img: Image16): Codee {
        val palette = ArrayList<Int>()
        val index = HashMap<Int, Int>()
        val numeros = img.pixels.map { c -> index.getOrPut(c) { palette.add(c); palette.size - 1 } }
        var bits = 1
        while ((1 shl bits) < palette.size) bits++
        val octets = ArrayList<Int>()
        var acc = 0L
        var n = 0
        for (i in numeros) {
            acc = acc or (i.toLong() shl n)
            n += bits
            while (n >= 8) { octets.add((acc and 0xFF).toInt()); acc = acc shr 8; n -= 8 }
        }
        if (n > 0) octets.add((acc and 0xFF).toInt())
        val rgb = palette.flatMap { listOf((it shr 16) and 0xFF, (it shr 8) and 0xFF, it and 0xFF) }
        return Codee(palette.size, rgb, octets)
    }

    /** Une « vignette » : en-tête 0xAA, taille, temps (ms depuis le début de l'animation), puis palette et pixels. */
    private fun vignette(img: Image16, temps: Int): List<Int> {
        val c = coder(img)
        val taille = 7 + c.pixels.size + c.palette.size
        return listOf(0xAA, taille and 0xFF, (taille shr 8) and 0xFF, temps and 0xFF, (temps shr 8) and 0xFF, 0, c.couleurs and 0xFF) +
            c.palette + c.pixels
    }

    /** Afficher une image fixe. */
    fun image(img: Image16, echapper: Boolean = false): ByteArray =
        message(IMAGE, listOf(0x00, 0x0A, 0x0A, 0x04) + vignette(img, 0), echapper)

    /** Une animation qui tourne en boucle sur le cadre : `ms` = durée de chaque image. Envoyée en morceaux de 200 octets. */
    fun animation(images: List<Image16>, ms: Int = 100, echapper: Boolean = false): List<ByteArray> {
        require(images.isNotEmpty()) { "animation vide" }
        val tout = images.flatMapIndexed { k, img -> vignette(img, k * ms) }
        val total = tout.size
        return tout.chunked(200).mapIndexed { i, morceau ->
            message(ANIMATION, listOf(total and 0xFF, (total shr 8) and 0xFF, i) + morceau, echapper)
        }
    }
}
