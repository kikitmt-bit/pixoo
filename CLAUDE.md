# pixoo — notes pour Claude

Brique de Kiki (pas développeur, réponses en français simples) pour piloter son cadre **Divoom Pixoo 16 × 16** en
**Bluetooth classique** (SPP / RFCOMM) depuis un téléphone Android. Dépôt **public** : jamais de secret ici.
Premier usage visé : le score du padel (`kikitmt-bit/padel_resa`) affiché sur le cadre pendant les séances, le
téléphone servant de relais (il lit le score dans Firebase, `/live/<clé>.json`, comme le tableau, et l'envoie au cadre).

- `android/pixoo` (bibliothèque Android, `fr.kiki.pixoo`) : `Image16` (16 × 16, 0xRRGGBB), `Police` (3 × 5, `echelle`),
  `Protocole` (Kotlin pur : messages, image, animation, luminosité ; option `echapper` pour les cadres plus récents),
  `Pixoo` (liaison Bluetooth en arrière-plan : la dernière image gagne, reconnexion toutes les 3 s, 1 s d'attente après
  la connexion, SPP puis canal 1 en secours).
- `android/essai` : appli « Pixoo essai » (Views simples, sans Compose).
- Protocole d'après pixoo-client (virtualabs) ; `ProtocoleTest` compare nos octets aux siens (générés avec son
  script Python sur le même motif). Ne jamais changer `Protocole` sans relancer ces tests.
- Fabrication : GitHub Actions (`android.yml`) : tests puis `pixoo-essai.apk` dans la Release « essai ».
  Pas de SDK Android dans le cloud : les parties pures se testent dans un petit projet Kotlin JVM (copier
  `Image16`, `Police`, `Protocole` et le test). Signature : `android/pixoo.keystore` (mot de passe dans `essai/build.gradle.kts`).
- **Pas encore essayé sur le vrai cadre** (le cloud ne joint pas le Bluetooth).
