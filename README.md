# pixoo

Piloter le cadre **Divoom Pixoo** (16 × 16 pixels, Bluetooth) depuis un téléphone Android, sans PC et sans l'appli Divoom.

- `android/pixoo/` : **la brique**, réutilisable par toutes les applis : se relier au cadre, afficher une image,
  jouer une animation, régler la luminosité. Petite police de chiffres incluse.
- `android/essai/` : **Pixoo essai**, une petite appli pour vérifier que tout marche avec le vrai cadre.

## Installer l'appli d'essai

1. Associer le cadre au téléphone : Réglages → Bluetooth → associer « Pixoo ».
2. Télécharger `pixoo-essai.apk` dans [Releases → essai](../../releases/tag/essai) sur le téléphone et l'installer.
3. L'ouvrir, autoriser « Appareils à proximité », toucher le cadre dans la liste, puis essayer les boutons.
   Rien ne s'affiche ? Cocher « Mode compatibilité ».

## Utiliser la brique dans une appli

```kotlin
val cadre = Pixoo(context, adresse)      // adresse : Pixoo.appareils(context)
cadre.quandEtat = { etat -> /* RELIE, CONNEXION, DECONNECTE */ }
cadre.relier()

val img = Image16()
img.ecrire("40", 0, 10, 0x2FA8F0)       // texte en petits chiffres 3 × 5
cadre.afficher(img)                      // seule la dernière image compte ; reconnexion toute seule
```

Le langage du cadre vient de [pixoo-client](https://github.com/virtualabs/pixoo-client) (virtualabs), réécrit en Kotlin ;
les tests vérifient qu'on envoie exactement les mêmes octets que lui.
