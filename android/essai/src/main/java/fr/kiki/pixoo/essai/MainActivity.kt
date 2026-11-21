package fr.kiki.pixoo.essai

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import fr.kiki.pixoo.Image16
import fr.kiki.pixoo.Pixoo

/** « Pixoo essai » : choisir le cadre parmi les appareils associés, puis lui envoyer des images de test.
 *  Sert à vérifier que la brique marche avec le vrai cadre avant de la brancher dans une vraie appli (le padel). */
class MainActivity : Activity() {
    private var cadre: Pixoo? = null
    private var lum = 60
    private lateinit var etat: TextView
    private lateinit var liste: LinearLayout
    private lateinit var essais: LinearLayout
    private val prefs by lazy { getSharedPreferences("pixoo", MODE_PRIVATE) }
    private val principal = Handler(Looper.getMainLooper())
    private var horloge: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 60) }
        page.addView(texte("Pixoo essai", 26f))
        page.addView(texte("1. Associe d'abord le cadre dans les réglages Bluetooth du téléphone.\n2. Touche-le ci-dessous.", 15f))
        etat = texte("", 17f).apply { setPadding(0, 30, 0, 30) }
        page.addView(etat)
        liste = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(liste)
        page.addView(bouton("↻ Revoir la liste") { remplirListe() })

        essais = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        essais.addView(texte("Essais", 20f).apply { setPadding(0, 40, 0, 10) })
        essais.addView(bouton("❤️ Les deux cœurs") { montrer(Dessins.coeurs()) })
        essais.addView(bouton("🎾 Un score d'exemple (4-3, 30-15)") { montrer(Dessins.score(4, 3, "30", "15")) })
        essais.addView(bouton("🌈 Mire de couleurs") { montrer(Dessins.mire()) })
        essais.addView(bouton("⏱️ Compteur qui avance (1 image par seconde)") { compteur() })
        essais.addView(bouton("🎞️ Animation qui tourne sur le cadre") { arreterCompteur(); cadre?.animer(Dessins.balle(), 90) })
        val lums = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        lums.addView(bouton("☀️ −") { lum = (lum - 20).coerceAtLeast(10); cadre?.luminosite(lum) }, LinearLayout.LayoutParams(0, -2, 1f))
        lums.addView(bouton("☀️ +") { lum = (lum + 20).coerceAtMost(100); cadre?.luminosite(lum) }, LinearLayout.LayoutParams(0, -2, 1f))
        essais.addView(lums)
        essais.addView(CheckBox(this).apply {
            text = "Mode compatibilité (si rien ne s'affiche)"
            textSize = 15f
            isChecked = prefs.getBoolean("echapper", false)
            setOnCheckedChangeListener { _, oui -> prefs.edit().putBoolean("echapper", oui).apply(); cadre?.echapper = oui }
        })
        page.addView(essais)
        setContentView(ScrollView(this).apply { addView(page) })

        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        } else demarrer()
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, resultats: IntArray) {
        if (resultats.firstOrNull() == PackageManager.PERMISSION_GRANTED) demarrer()
        else etat.text = "Il faut autoriser « Appareils à proximité » pour parler au cadre (Réglages → Applis → Pixoo essai)."
    }

    private fun demarrer() {
        remplirListe()
        prefs.getString("adresse", null)?.let { choisir(it) }
    }

    private fun remplirListe() {
        liste.removeAllViews()
        val appareils = Pixoo.appareils(this)
        if (appareils.isEmpty()) { etat.text = "Aucun appareil associé. Associe le cadre dans les réglages Bluetooth."; return }
        for (d in appareils) liste.addView(bouton((if (Pixoo.ressembleAUnCadre(d)) "🟦 " else "") + nom(d)) { choisir(d.address) })
    }

    private fun choisir(adresse: String) {
        arreterCompteur()
        cadre?.fermer()
        prefs.edit().putString("adresse", adresse).apply()
        etat.text = "Connexion au cadre…"
        cadre = Pixoo(this, adresse).apply {
            echapper = prefs.getBoolean("echapper", false)
            quandEtat = { e ->
                etat.text = when (e) {
                    Pixoo.Etat.RELIE -> "✅ Relié au cadre"
                    Pixoo.Etat.CONNEXION -> "Connexion au cadre…"
                    Pixoo.Etat.DECONNECTE -> "❌ Pas relié : cadre allumé et près du téléphone ? Nouvel essai dans 3 s…"
                }
            }
            relier()
            luminosite(lum)
            afficher(Dessins.coeurs())
        }
        essais.visibility = View.VISIBLE
    }

    private fun montrer(img: Image16) { arreterCompteur(); cadre?.afficher(img) }

    private fun compteur() {
        arreterCompteur()
        var n = 0
        val r = object : Runnable {
            override fun run() { cadre?.afficher(Dessins.nombre(n++ % 100)); principal.postDelayed(this, 1_000) }
        }
        horloge = r
        principal.post(r)
    }

    private fun arreterCompteur() { horloge?.let { principal.removeCallbacks(it) }; horloge = null }

    override fun onDestroy() { arreterCompteur(); cadre?.fermer(); super.onDestroy() }

    private fun nom(d: BluetoothDevice) = try { d.name ?: d.address } catch (e: SecurityException) { d.address }

    private fun texte(t: String, taille: Float) = TextView(this).apply { text = t; textSize = taille; setTextColor(Color.WHITE) }

    private fun bouton(t: String, action: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; textSize = 16f; gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setOnClickListener { action() }
    }
}
