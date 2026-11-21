package fr.kiki.pixoo.essai

import fr.kiki.pixoo.Image16
import fr.kiki.pixoo.Police

/** Les images de test de l'appli d'essai. */
object Dessins {
    private const val BLEU = 0x2FA8F0
    private const val ROUGE = 0xE01010
    private const val JAUNE = 0xF8D020

    /** Les deux cœurs de la photo du cadre. */
    fun coeurs(): Image16 = Image16.dessin(
        listOf(
            "................",
            ".##..##.........",
            "#RR##RR#........",
            "#RRRRRR#........",
            "#RRRRRR#........",
            ".#RRRR#.........",
            "..#RR#..........",
            "...##...........",
            "................",
            ".........##.##..",
            "........#RR#RR#.",
            "........#RRRRR#.",
            ".........#RRR#..",
            "..........#R#...",
            "...........#....",
            "................",
        ),
        mapOf('#' to Image16.NOIR, 'R' to ROUGE),
        fond = BLEU,
    )

    /** Un score : jeux en haut (petit), points en bas (sur toute la largeur). Bleu = nous, rouge = eux. */
    fun score(jeuxA: Int, jeuxB: Int, ptsA: String, ptsB: String): Image16 {
        val img = Image16()
        img.ecrire("$jeuxA", 4, 1, BLEU)
        img.rect(7, 3, 2, 1, 0x606060)
        img.ecrire("$jeuxB", 9, 1, ROUGE)
        img.rect(0, 7, 16, 1, 0x303030)
        img.ecrire(ptsA, 0, 10, BLEU)
        img.ecrire(ptsB, 16 - Police.largeur(ptsB), 10, ROUGE)
        return img
    }

    /** 16 bandes de couleur : pour voir si les couleurs arrivent justes. */
    fun mire(): Image16 {
        val couleurs = intArrayOf(0xFF0000, 0xFF8000, 0xFFFF00, 0x80FF00, 0x00FF00, 0x00FF80, 0x00FFFF, 0x0080FF,
            0x0000FF, 0x8000FF, 0xFF00FF, 0xFF0080, 0xFFFFFF, 0xA0A0A0, 0x505050, 0x000000)
        val img = Image16()
        for (x in 0 until 16) img.rect(x, 0, 1, 16, couleurs[x])
        return img
    }

    /** Un nombre de 0 à 99 en grand (police ×2), centré. */
    fun nombre(n: Int): Image16 {
        val img = Image16()
        val t = "$n"
        val l = Police.largeur(t, 2)
        img.ecrire(t, (16 - l) / 2, 3, JAUNE, 2)
        return img
    }

    /** Une balle de padel qui rebondit : 12 images, envoyées une fois, puis le cadre la joue tout seul. */
    fun balle(): List<Image16> = (0 until 12).map { k ->
        val img = Image16(0x103060)
        img.rect(0, 14, 16, 2, 0x20A040)
        val t = k / 11.0
        val x = 1 + (t * 12).toInt()
        val y = 11 - (12 * 4 * t * (1 - t)).toInt().coerceAtMost(11)
        img.rect(x, y, 3, 3, JAUNE)
        img
    }
}
