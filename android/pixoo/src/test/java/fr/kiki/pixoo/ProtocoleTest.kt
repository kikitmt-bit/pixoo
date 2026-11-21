package fr.kiki.pixoo

import org.junit.Assert.assertEquals
import org.junit.Test

/** Les octets attendus ont été produits par pixoo-client (Python, virtualabs), qui marche sur un vrai Pixoo :
 *  si ces tests passent, on envoie exactement la même chose que lui. */
class ProtocoleTest {
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    private fun motif(): Image16 {
        val img = Image16()
        for (y in 0 until 16) for (x in 0 until 16) {
            if ((x + y) % 3 == 1) img[x, y] = 0xFF0000
            else if ((x * y) % 5 == 2) img[x, y] = 0x0080FF
        }
        return img
    }

    @Test fun imageCommePixooClient() {
        assertEquals(
            "01570044000a0a04aa500000000003000000ff00000080ff04411004619004411824411004411824411006491004411024411006491084411006491084411204411004411084411204619004411204619004411804411004270f02",
            hex(Protocole.image(motif())),
        )
    }

    @Test fun luminositeCommePixooClient() {
        assertEquals("0104007450c80002", hex(Protocole.luminosite(80)))
    }

    @Test fun uneSeuleCouleurTientSurUnBit() {
        val c = Protocole.coder(Image16(0x123456))
        assertEquals(1, c.couleurs)
        assertEquals(32, c.pixels.size)  // 256 pixels × 1 bit
    }

    @Test fun echappementNeLaissePlusDe01a03AuMilieu() {
        val m = Protocole.image(motif(), echapper = true)
        assertEquals(0x01, m.first().toInt())
        assertEquals(0x02, m.last().toInt())
        val milieu = m.drop(1).dropLast(1).map { it.toInt() and 0xFF }
        var i = 0
        while (i < milieu.size) {
            if (milieu[i] == 0x03) { assert(milieu[i + 1] in 0x04..0x06); i += 2 } else { assert(milieu[i] !in 0x01..0x02); i++ }
        }
    }

    @Test fun animationEnMorceauxDe200() {
        val images = List(6) { motif() }
        val morceaux = Protocole.animation(images, 120)
        val total = 6 * (7 + 3 * 3 + 64)
        assertEquals((total + 199) / 200, morceaux.size)
        assertEquals(0x49, morceaux[0][3].toInt())
        assertEquals(total and 0xFF, morceaux[0][4].toInt() and 0xFF)
    }

    @Test fun policeLogeDeuxScoresSur16() {
        assertEquals(7, Police.largeur("40"))
        val img = Image16()
        img.ecrire("40", 0, 0, 0xFFFFFF)
        img.ecrire("15", 9, 0, 0xFFFFFF)
        assertEquals(0xFFFFFF, img[0, 0])   // le « 4 » commence en haut à gauche
        assertEquals(0xFFFFFF, img[15, 4])  // le « 5 » finit dans le coin
    }
}
