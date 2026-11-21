package fr.kiki.pixoo

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

/** Le cadre Pixoo, relié en Bluetooth au téléphone. Le cadre doit d'abord être associé une fois dans les réglages
 *  Bluetooth d'Android, et l'appli doit avoir la permission « Appareils à proximité » (BLUETOOTH_CONNECT, Android 12+).
 *
 *  Tout se passe en arrière-plan : `afficher` ne bloque jamais. Si on envoie plusieurs images d'affilée, seule la
 *  dernière compte (le score le plus récent). Si la liaison tombe (cadre éteint, trop loin), on se relie tout seul
 *  toutes les 3 s et on renvoie la dernière image.
 *
 *      val cadre = Pixoo(context, adresse)          // adresse : voir Pixoo.appareils(context)
 *      cadre.quandEtat = { etat -> ... }            // sur le fil principal
 *      cadre.relier()
 *      cadre.afficher(image)                        // une Image16
 *      cadre.fermer()                               */
@SuppressLint("MissingPermission")
class Pixoo(context: Context, val adresse: String) {
    enum class Etat { DECONNECTE, CONNEXION, RELIE }

    /** Appelé (sur le fil principal) à chaque changement d'état de la liaison. */
    var quandEtat: (Etat) -> Unit = {}
    /** Pour les cadres plus récents qui veulent les octets « échappés » : à essayer si rien ne s'affiche. */
    @Volatile var echapper = false

    @Volatile var etat = Etat.DECONNECTE
        private set
    /** Pourquoi la dernière connexion a échoué (une ligne par façon essayée), pour le diagnostic. */
    @Volatile var derniereErreur: String? = null
        private set
    /** La façon de se relier qui a marché. */
    @Volatile var methode: String? = null
        private set

    private val adaptateur = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val fil = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())
    @Volatile private var prise: BluetoothSocket? = null
    @Volatile private var ouvert = false
    @Volatile private var aEnvoyer: (() -> List<ByteArray>)? = null  // la dernière chose demandée (image ou animation)
    @Volatile private var luminosite: Int? = null
    @Volatile private var enAttente = false
    private val reessai = Runnable { travailler() }

    /** Se relier au cadre (et rester relié jusqu'à `fermer`). */
    fun relier() {
        ouvert = true
        travailler()
    }

    /** Afficher une image fixe. */
    fun afficher(img: Image16) {
        val copie = img.copie()
        aEnvoyer = { listOf(Protocole.image(copie, echapper)) }
        travailler()
    }

    /** Une animation qui tourne en boucle sur le cadre (`ms` par image). */
    fun animer(images: List<Image16>, ms: Int = 100) {
        val copies = images.map { it.copie() }
        aEnvoyer = { Protocole.animation(copies, ms, echapper) }
        travailler()
    }

    /** Luminosité de 0 à 100 (gardée et renvoyée après chaque reconnexion). */
    fun luminosite(pct: Int) {
        luminosite = pct.coerceIn(0, 100)
        travailler()
    }

    fun fermer() {
        ouvert = false
        principal.removeCallbacks(reessai)
        fil.execute { couper() }
    }

    // ce qui a déjà été envoyé sur la liaison actuelle (pour ne pas renvoyer deux fois la même chose)
    @Volatile private var dejaEnvoye: Any? = null
    @Volatile private var lumEnvoyee: Int? = null

    private fun travailler() {
        if (!ouvert || enAttente) return
        enAttente = true
        fil.execute {
            enAttente = false
            try {
                val p = prise ?: brancher()
                luminosite?.let { if (it != lumEnvoyee) { ecrire(p, Protocole.luminosite(it, echapper)); lumEnvoyee = it } }
                val quoi = aEnvoyer
                if (quoi != null && quoi !== dejaEnvoye) {
                    for (m in quoi()) { ecrire(p, m); if (m.size > 100) Thread.sleep(30) }  // animation : on laisse respirer le cadre
                    dejaEnvoye = quoi
                }
            } catch (e: Exception) {
                couper()
                if (ouvert) { principal.removeCallbacks(reessai); principal.postDelayed(reessai, 3_000) }
            }
        }
    }

    private fun brancher(): BluetoothSocket {
        changer(Etat.CONNEXION)
        val appareil = adaptateur?.getRemoteDevice(adresse) ?: throw IOException("pas de Bluetooth")
        adaptateur.cancelDiscovery()
        // plusieurs façons de se relier : selon les cadres et les téléphones, une seule marche
        val essais = listOf<Pair<String, () -> BluetoothSocket>>(
            "SPP" to { appareil.createRfcommSocketToServiceRecord(SPP) },
            "SPP sans sécurité" to { appareil.createInsecureRfcommSocketToServiceRecord(SPP) },
            "canal 1" to { canal(appareil, "createRfcommSocket", 1) },
            "canal 1 sans sécurité" to { canal(appareil, "createInsecureRfcommSocket", 1) },
            "canal 2" to { canal(appareil, "createRfcommSocket", 2) },
        )
        var p: BluetoothSocket? = null
        val erreurs = ArrayList<String>()
        for ((nom, creer) in essais) {
            var s: BluetoothSocket? = null
            try {
                s = creer(); s.connect(); p = s; methode = nom; break
            } catch (e: Exception) {
                try { s?.close() } catch (_: IOException) {}
                erreurs.add("$nom : ${(e.cause ?: e).message ?: e.javaClass.simpleName}")
            }
        }
        if (p == null) {
            derniereErreur = erreurs.joinToString("\n")
            throw IOException(derniereErreur)
        }
        derniereErreur = null
        Thread.sleep(1_000)  // le cadre veut au moins 1 s avant le premier message
        prise = p
        dejaEnvoye = null
        lumEnvoyee = null
        changer(Etat.RELIE)
        return p
    }

    private fun canal(d: BluetoothDevice, fonction: String, n: Int): BluetoothSocket =
        d.javaClass.getMethod(fonction, Int::class.javaPrimitiveType).invoke(d, n) as BluetoothSocket

    private fun ecrire(p: BluetoothSocket, octets: ByteArray) {
        p.outputStream.write(octets)
        p.outputStream.flush()
    }

    private fun couper() {
        try { prise?.close() } catch (_: IOException) {}
        prise = null
        changer(Etat.DECONNECTE)
    }

    private fun changer(e: Etat) {
        if (etat == e) return
        etat = e
        principal.post { quandEtat(e) }
    }

    companion object {
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Les appareils associés au téléphone, ceux qui ressemblent à un cadre Divoom en premier. */
        fun appareils(context: Context): List<BluetoothDevice> {
            val ad = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return emptyList()
            val tous = try { ad.bondedDevices?.toList() ?: emptyList() } catch (e: SecurityException) { emptyList() }
            return tous.sortedBy { if (ressembleAUnCadre(it)) 0 else 1 }
        }

        fun ressembleAUnCadre(d: BluetoothDevice): Boolean {
            val nom = (try { d.name } catch (e: SecurityException) { null } ?: "").lowercase()
            return listOf("pixoo", "divoom", "timebox", "ditoo", "tivoo").any { it in nom }
        }
    }
}
