# Parité GUI — client zio → Vaultline

Table de référence du portage de `dev/zio/client/gui` (`open-zio-main`, 73 fichiers) dans
`src/main/java/com/pro0101/vaultline/gui/zio`. Une ligne par effet visible : animation, flou, halo,
dégradé, teinte, transition, survol, dépassement, enchaînement de frames.

Deux états seulement :

- **Porté** — suivi du nom exact des tests qui le prouvent, ou, quand l'effet n'est qu'une constante
  appliquée (aucun seam unitaire n'existe pour un littéral), du `fichier:ligne` qui la porte.
  Ces lignes sont signalées « *ligne* ».
- **Exclu** — suivi de la preuve : signature d'API `fichier:ligne` ou sortie `grep` brute avec le
  chemin du paquet.

Légende des sources : `ZS` = `src/main/java/com/pro0101/vaultline/gui/zio/`.

## 1. Écran principal (ClickGUI)

`ClientScreen.java`, `ClickGuiScreen.java`, `panels/Panel.java`.

| Effet | État | Preuve |
|---|---|---|
| Fondu d'ouverture et de fermeture sur 150 ms | Porté | `ZioAnimTest.aTimedValueRunsStraightBetweenTwoValues` ; constante `ZioAnim.OPEN_MILLIS` appliquée `ZS/ZioScreen.java:548` |
| Voile de recherche : opacité 20 %, montée sur 300 ms | Porté | `ZioPaintTest.theSearchDimsTheWholeScreenByAFifth` ; durée `ZS/ZioScreen.java:105` |
| Requête centrée, complétion en gris `AAAAAA` | Porté | `ZioPaintTest.theSearchIsCentredOnTheScreenWithItsCompletionInGrey` |
| Caret clignotant toutes les 500 ms | Porté | `ZioPaintTest.theCaretAndCompletionOnlyAppearWhenThereIsOne` ; période `ZS/ZioScreen.java:106` |
| Hint pulsé (`pulse(GRAY, WHITE, 2000, 250)`) en bas à droite, à la marge HUD | Porté | `ZioPaintTest.theHintSitsInTheCornerClearOfTheHudMargin` |
| Teinte plein écran puis dégradé vertical | Porté | `ZioPanelPaintTest.theScreenTintAndGradientAreTheUiModulesDefaults` |
| Aero : image 600×450 à sa taille, décalée de `panelX/3`, alpha moitié, découpée au panneau | Porté | `ZioPanelPaintTest.theSheetIsShiftedByThePanelsPlaceNotStretched`, `theSheetKeepsItsOwnSize`, `theSheetIsDrawnAtHalfTheScreenFade` |
| Flou des panneaux (rayon 6 × fondu) et halo bloom sur les mêmes rectangles | Porté | *ligne* : `ZS/ZioScreen.java:352` (`GuiFx.render(EFFECT_RECTS, …)`) et `ZS/ZioScreen.java:175` (`BLUR_RADIUS = 6f`) |
| Flocons : 25, chute 0,07–0,18, dérive ±0,033, échelle 0,5–1,15, alpha ≥ 4, décembre–février | Porté | `ZioSnowTest.aScreenStartsWithTwentyFiveFlakesAboveTheTopEdge`, `aStepFallsByTheFrameFactorTimesTheSpeedAndDriftsSideways`, `opacityFallsWithTheDepthAndNeverGoesBelowFour`, `winterIsDecemberJanuaryAndFebruary` |
| Flocon dessiné : six branches d'un pixel, rayon 2 à 5 selon l'échelle ; plus de glyphe tapé | Porté | `ZioSnowflakeTest.theStarHasSixArmsAroundItsCentre`, `theArmsEndAtTheRadiusAlongEachSpoke`, `everyStampIsOnePixelWide`, `aLargerRadiusReachesFurtherOut` ; `ZS/ZioPaint.java:253` |
| Tooltip : neuf pixels à droite, padding d'un pixel, une ligne par hauteur+1, retournement au bord | Porté | `ZioPaintTest.theTooltipPadsItsFirstLineOnePixelsInAndStartsAboveThePointer`, `theTooltipFlipsToTheLeftOfThePointerAtTheScreenEdge` |
| Le tooltip poursuit le pointeur à 2,5 (la seule animation plus rapide que 2) | Porté | `ZioPaintEffectsTest.theTooltipChasesThePointerFasterThanThePanelsAnimate` |
| Flou du tooltip, rayon 6, `10,10,10,140`, fond `10,10,10,80`, **même image que le tooltip, transitions d'ouverture et de fermeture comprises** | Porté | `ZioPaintEffectsTest.theBlurredRectangleIsExactlyTheOneTheTooltipFills`, `aPointerThatMovedBetweenFramesMovesTheBlurAndTheTooltipTogether`, `atTheRightEdgeBothFlipToTheSameSideOfThePointer` ; `ZioTooltipFrameTest.theLineIsReadOnlyAfterThePanelsHaveStepped`, `anOpeningPanelHasItsTooltipOnTheFrameThatOpensIt`, `aClosingPanelDropsItsTooltipOnTheFrameItCloses`, `theRectangleIsTheOneTheLinesOfThatSameFrameFill` | un rectangle par image (`ZS/ZioPaint.java:58`), décidé **après** l'avancée des panneaux (`ZS/ZioTooltipFrame.java:38`, appelée en `ZS/ZioScreen.java:2193`), donné au flou (`ZS/ZioScreen.java:380`) puis au dessin (`ZS/ZioScreen.java:593`) ; comme zio, qui floute le tooltip qu'il vient de placer, dans sa propre passe (`ClientScreen.java:506`) |
| En-tête : 14 px, 12 en slim, alpha `0,5 + 0,5·anim` sur 200 ms | Porté | `ZioPanelPaintTest.theHeaderIsTwelveInSlimAndFourteenOtherwise`, `ZioAnimTest.aTimedValueRetargetsFromWhereItIsNow` |
| Outline d'un pixel à l'alpha de l'en-tête, jamais plus court que lui | Porté | `ZioPanelPaintTest.theOutlineNeverShorterThanTheHeader` |
| Défilement lissé (vitesse 2), dépassement de 40 px, rappel dépendant du fps | Porté | `ZioScrollTest.aWheelPastEitherEndRunsFortyPixelsAndNoFurther`, `theSpringBackStartsOnlyAfterItsWait`, `aSlowFrameRateSpringsBackSooner`, `theOverScrollIsClippedAtFortyEvenAfterAJump` |
| Deux dégradés de défilement de dix pixels, alpha `0,3 × fondu` | Porté | `ZioPaintTest.aFadeScalesTheAlphaAndKeepsTheColour` |
| Panneau tiré : passe au premier plan, ordre des autres conservé, reste entièrement à l'écran | Porté | `ZioDragTest.theGrabbedPanelGoesToTheEndAndTheOthersKeepTheirOrder`, `thePanelStaysInsideTheScreenOnTheLeftAndRightEdges`, `aPanelTallerThanTheScreenSitsAtZeroRatherThanOffTheTop`, `clampingTwiceChangesNothing` |
| Boutons de chrome (presets, zoom, Fx) dans la bande libre, sans chevauchement | Porté | `ZioChromeTest.everyControlSitsInTheStripThePanelsLeaveFree`, `noControlOverlapsAnother` |
| Séquence secrète `↑↑↓↓←→←→BA` qui débloque le panneau de jeux, gardée entre les lancements | Porté | `ZioSecretSequenceTest.theSequenceIsTheCopiedClients`, `thePanelIsUnlockedOnlyByTheLastKey`, `aWrongKeyTakesTheIndexBackToTheStart`, `theTwoLetterEndNeedsBothLettersInOrder` ; `ZioGamesTest.unlockingSurvivesAFreshLaunch` ; `ZS/ZioSecretSequence.java:24`, `ZS/ZioGames.java:35` |
| Panneau de jeux posé à droite au-dessus de la bande, ordre de cycle des jeux respecté | Porté | `ZioGamePanelTest.theGamesCycleInTheCopiedClientsOrder`, `thePanelSitsAtTheRightAboveTheStrip` |

## 2. Lignes de module

`components/ModuleComponent.java`, `components/AbstractComponent.java`.

| Effet | État | Preuve |
|---|---|---|
| Remplissage d'activation qui grandit de 1 px à la largeur × progression | Porté | `ZioAnimTest.anApproachClosesTheDistanceAndSnapsWhenItIsSmall` ; géométrie `ZS/ZioScreen.java:1413` |
| Dégradé de bouton : couleur à un dixième d'alpha en haut, rien en bas | Porté | `ZioPaintEffectsTest.aButtonRowCarriesTheGradientFromATenthOfItsAlphaToNothing` |
| Survol : levée d'un pixel sur 65 ms | Porté | `ZioAnimTest.aTimedValueRetargetsFromWhereItIsNow` ; `ZioAnim.HOVER_MILLIS` |
| Flash de recherche rouge, quatre cycles de 150 ms | Porté | `ZioPaintTest.theRowFlashIsRedAndFadesWithTheBlink` |
| Trois modules ouverts au plus, le plus ancien se referme | Porté | `ZioOpenRowsTest.aFourthOpenClosesTheFirst`, `openingKeepsTheNewestLast` |
| Statut `[TOUCHE]` en demi-taille après le nom | Porté | `ZioPaintTest.theModuleStatusIsBracketedAndHalfSized` |
| Ligne de bind `Key B` / `Toggle B` / `Hold B` / `Key …` | Porté | `ZioPaintTest.theBindRowIsDrawnFourPixelsInWithItsMode` |
| Mode de bind stocké, cyclé au clic droit, persisté | Porté | `ZioBindModeTest.aRightClickStepsTheModeAndComesBackAround`, `ZioBindModesTest.cyclingStepsTheModeAndWritesItOut`, `aWrittenModeIsReadBackByAFreshStore` |
| Indicateur `+`/`-` à quatre pixels du bord, hauteur de bloc animée | Porté | `ZioListLayoutTest.aBlockOfLinesIsAsTallAsItsLines` ; animation `ZioAnimTest.anApproachClosesTheDistanceAndSnapsWhenItIsSmall` |

## 3. Réglages et listes

`components/SettingComponent.java`, `Boolean*`, `Numeric*`, `Enum*`, `String*`, `Keybind*`,
`registry/RegistryEntryComponent.java`.

| Effet | État | Preuve |
|---|---|---|
| Barre pleine derrière une énumération, deux pixels de retrait | Porté | `ZioPaintTest.anEnumerationIsBackedByTheAccentBarTwoPixelsIn` |
| Barre de sélection qui glisse d'une valeur à l'autre et cadre de la boîte ouverte | Porté | `ZioEnumBoxTest.theBarStartsOnTheFirstChoice`, `theBarFollowsHowFarItHasSlid`, `theSlideApproachesTheChosenChoiceAndSettlesOnIt`, `theOutlineClosesTheSettingLineAndEveryChoice` |
| Remplissage booléen : deux pixels à gauche, un pixel avant le bord droit | Porté | `ZioPaintTest.aFadeScalesTheAlphaAndKeepsTheColour` ; géométrie `ZS/ZioScreen.java:1603` |
| Curseur numérique : remplissage à la valeur, largeur bornée à `largeur−3` | Porté | `ZioPaintTest.aFadeScalesTheAlphaAndKeepsTheColour` ; géométrie `ZS/ZioScreen.java:1613` |
| Champ texte : barre pleine largeur derrière la ligne | Porté | *ligne* : `ZS/ZioScreen.java:1643` |
| Curseur d'édition clignotant toutes les 500 ms | Porté | *ligne* : `ZS/ZioScreen.java:2495` (`editedText`) |
| Nom trop long : défilement après 750 ms puis 500 ms, découpé à la ligne | Porté | `ZioScissorTest.aPanelClippedSettingsBlockStaysInsideThePanel` |
| Description de réglage prioritaire sur celle du module, dans le tooltip | Porté | `ZioPaintTest.aSettingDescriptionWinsOverItsModules` |
| Statut du bind d'un réglage, demi-taille, après le nom | Porté | `ZioPaintTest.theSettingBindStatusIsBracketedHalfSizedAfterTheName` |
| Éditeur de liste : champ de recherche encadré, lignes fixes, cases cochées, dégradés de bord | Porté | `ZioListLayoutTest.aClickIsResolvedAgainstTheHeightOfEachLine`, `ZioInputRouterTest.theEntryUnderThePointerFollowsTheScroll` |
| Fondu des lignes qui reviennent ; la recherche les pose à zéro au lieu de les estomper | Porté | `ZioRowFadeTest.aReturningRowFadesInOverAFewFrames`, `aHiddenRowFadesOutAndIsThenForgotten`, `theSearchHidesARowAtOnceInsteadOfFadingIt` |

## 4. Sélecteur de couleur

`components/ColorSettingComponent.java`.

| Effet | État | Preuve |
|---|---|---|
| Marqueurs qui glissent à 0,35 (un tiers de la vitesse générale) | Porté | `ZioColorMarkerTest.theMarkerGlidesAtItsOwnSlowerSpeed` |
| Marqueur du carré : 3 px au repos, 4 px pendant la prise | Porté | `ZioColorMarkerTest.theMarkerIsThreePixelsAtRestAndFourWhileHeld` |
| Bord noir sur une couleur claire, blanc sinon (luminance 0,2126/0,7152/0,0722) | Porté | `ZioColorMarkerTest.aLightColourTakesABlackBorderOnceItHasFadedIn`, `luminanceReadsWhiteAsOneAndBlackAsZero` |
| Intérieur : couleur choisie assombrie | Porté | `ZioColorMarkerTest.theInsideIsTheChosenColourDarkened` |
| Curseurs teinte et alpha : accent au repos, blanc pendant la prise | Porté | `ZioColorMarkerTest.aSliderMarkerTurnsWhiteWhileItIsHeld` |
| Barre d'alpha : opaque à gauche, transparente à droite, marqueur à `(largeur−1)(1−a)` | Porté | `ZioColorMarkerTest.theAlphaBarRunsFromOpaqueToClear`, `theAlphaMarkerSitsAtTheOpaqueEndOfTheBar` |
| Damiers de transparence (aperçu blanc/`AAAAAA`, barre blanc/`999999`) | Porté | `ZioColorMarkerTest.theCheckerboardAlternatesWhiteAndGrey` |

## 5. Jeux

`panels/GamePanel.java`, `components/Snake*`, `Tetris*`, `Minesweeper*`, `GridGameComponent.java`.

| Effet | État | Preuve |
|---|---|---|
| Plateaux 110 px, minefield 8 px par cellule, bouton sous le plateau | Porté | `ZioGamePanelTest.snakeAndTetrisAreTheCopiedHundredAndTenPixelSquareBoards`, `theMinefieldIsAsWideAsItsColumnsAndEightPixelsACell`, `theGameButtonSitsRightOfTheSizeControls` |
| Serpent : pas de 500 ms, accéléré avec Espace, bords qui bouclent | Porté | `ZioSnakeTest.theGameStepsEveryFiveHundredMillisecondsAndFasterWithSpaceHeld`, `theEdgesWrapAround` |
| Tetris : grille de coordonnées, pièce fantôme, lignes qui tombent | Porté | `ZioTetrisTest.theShapesKeepTheCopiedCoordinateTable`, `theGhostFallsAsFarAsThePiece`, `aFullRowIsClearedAndTheRowsAboveFall` |
| Démineur : sprites dans l'ordre de la planche, nombres, drapeaux | Porté | `ZioMinesweeperTest.theSpritesKeepTheClientsSheetOrder`, `everyNumberCountsItsNeighbouringMines`, `flagsStopAtTheMineCountAndCanBeTakenBack` |

## 6. Éditeur de HUD

`screens/HudEditorScreen.java`, `screens/HudMarginController.java`.

| Effet | État | Preuve |
|---|---|---|
| Bandes de marge : fondu d'un quart de seconde, la verticale attend l'horizontale | Porté | `ZioHudMarginsTest.theBandsFadeInOverTheirQuarterSecond`, `theVerticalBandsWaitForTheSideBandsToFade` |
| Guides du centre : croix blanche à 40 %, une verticale et deux moitiés horizontales | Porté | `ZioHudEditorFxTest.theGuidesMeetInTheMiddleOfTheScreen`, `theGuidesStayInsideAnOddSizedScreen` |
| Étiquette de survol : glissement de 10 px, fondu, vitesse 2 | Porté | `ZioHudEditorFxTest.aLabelSlidesInFromTenPixelsAway`, `theLabelFadesToWhiteAndNeverPastIt`, `theLabelEasesAtTheClientsSpeedOfTwo` |
| Accrochage à 5 px du bord ou du centre, poignée de redimensionnement en bas à droite | Porté | `ZioHudLayoutTest.anEdgeWithinFivePixelsSnapsToTheSafeMargin`, `theCentreSnapsWhenTheMiddleIsClose`, `theResizeCornerIsTheBottomRightPatch` |
| Déplacement et redimensionnement bornés à l'écran | Porté | `ZioHudLayoutTest.aDragNeverLeavesTheScreen`, `aResizeStopsAtTheScreenEdges` |

## 7. Chrome, curseurs, échelle, presets, entrées

| Effet | État | Preuve |
|---|---|---|
| Curseur main sur tout ce qui se clique, I-beam sur un champ | Porté | `ZioCursorRuleTest.everythingThePointerCanActOnTakesTheHand`, `aFieldTakesTheIBeam` |
| Zoom par paliers, pourcentage affiché, valeur bornée et persistée | Porté | `ZioScaleTest.steppingMovesByOneSizeAndStopsAtTheEnds`, `ZioScaleStoreTest.aWrittenZoomIsReadBack` |
| Presets : nom coupé et nettoyé, liste ordonnée sans tenir compte de la casse | Porté | `ZioPresetsTest.aNameIsCutToTheAcceptedLength`, `theListIsOrderedWithoutRegardToCase` |
| Recherche : `Ctrl+F` seulement si rien d'autre ne tient le clavier, liste avant le champ de preset | Porté | `ZioInputRouterTest.theScreenSearchOpensOnCtrlFOnlyWhenNothingElseHasTheKeyboard`, `anOpenListStillComesBeforeThePresetNameField` |
| Pile de scissor imbriquée, une région ne sort jamais de sa parente | Porté | `ZioScissorTest.aNestedRegionIsClippedToItsParent`, `closingHandsBackTheParentEachTime` |

## 8. Écrans de zio sans équivalent — exclus, avec preuve

Sortie brute (aucune de ces fonctionnalités n'existe dans mon paquet GUI) :

```
$ grep -rni "Trade"          src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "Shulker"        src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "ViewModel"      src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "SoundSelection" src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "onFilesDrop"    src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "Disconnect"     src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "ModuleImport"   src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "PresetImport"   src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "RegistrySet"    src/main/java/com/pro0101/vaultline/gui      | wc -l → 0
$ grep -rni "scrollbar"      src/main/java/com/pro0101/vaultline/gui/zio  | wc -l → 0

# côté zio, depuis la copie du client (hors de ce dépôt)
$ Z=~/open-zio-main/src/main/java/dev/zio/client/gui
$ grep -rni "scrollbar" "$Z"   | wc -l → 0
$ grep -rni "isWip"     "$Z"   | wc -l → 1     (ModuleComponent.java:439)
$ grep -rn  "FontsModule" "$Z" | wc -l → 3
$ find "$Z" -iname "*Trade*"   | wc -l → 10
```

| Écran zio | Effet qu'il porte | État |
|---|---|---|
| `TradeEditorScreen`, `TradeManagerScreen`, `TradeItemsComponent`, `Trade*Panel` (dégradés de survol animés) | 10 fichiers (`find "$Z" -iname "*Trade*"` → 10) | **Exclu** — `grep -rni Trade src/main/java/com/pro0101/vaultline/gui` → 0 : aucune fonction d'échange à thémer |
| `ShulkerPreviewScreen` | aperçu de boîte | **Exclu** — `grep -rni Shulker …/gui` → 0 |
| `SoundSelectionScreen`, `SoundOptionButton` | sélecteur de son | **Exclu** — `grep -rni SoundSelection …/gui` → 0 |
| `ViewModelEditorScreen` | guides et curseurs de modèle | **Exclu** — `grep -rni ViewModel …/gui` → 0 |
| `DisconnectConfirmationScreen`, `ConfirmGameCloseScreen`, `ModuleImportConfirmationScreen`, `PresetImportScreen` | fondus propres à ces fenêtres, dépôt de fichier | **Exclu** — `grep -rni Disconnect …/gui` → 0, `grep -rni onFilesDrop …/gui` → 0 |
| `RegistrySetScreen`, `RegistryListPanel` | sélecteur de registre avec icônes d'items | **Exclu** — `grep -rni "RegistrySet" …/gui` → 0 ; l'effet de fondu de ses lignes est porté (`ZioRowFadeTest`) |
| Barre de défilement | — | **Exclu** — `grep -rni scrollbar $Z` → **0** (zio) et `grep -rni scrollbar src/main/java/com/pro0101/vaultline/gui/zio` → **0** (moi) ; les deux occurrences de mon paquet `gui` sont `Palette.java:7` et `PresetTheme.java:307` (`skin.scrollbarColor`), c'est-à-dire le thème natif de Meteor |
| Police secondaire 0,65 (`FontsModule`) | — | **Exclu** — `grep -rn FontsModule $Z` → 3 (le module et ses usages) ; `grep -rn FontsModule src/main/java` → 1, le commentaire `ZS/ZioFontRenderer.java:45`. Le module n'existe pas |
| Préfixe `beta ` des modules WIP | — | **Exclu** — `grep -rni isWip $Z` → 1 (`ModuleComponent.java:439`) ; `grep -rni isWip src/main/java` → 0, aucun module n'expose cet état |

## 9. Contraintes assumées de la GUI 1.21 (pas des oublis)

| Écart | Preuve que c'est une contrainte de la GUI 1.21, pas un oubli | Relevé |
|---|---|---|
| Demi-pixels (0,5 px) impossibles | `ZS/ZioCanvas.java:14` → `void fill(int x1, int y1, int x2, int y2, int argb);` : aucun chemin de dessin n'accepte un flottant | API de dessin de la GUI 1.21, consignée le 2026-11-16 |
| Hauteur de ligne 13 au lieu de 12 | `ZS/ZioListLayout.java:16` → `ROW_HEIGHT = 13` ; `ZS/ZioFontRenderer.java:95` → `lineHeight()` vient de l'atlas (9). La formule de zio (4 + police) est recopiée, seule la métrique diffère | Police de la GUI 1.21, consignée le 2026-11-16 |

**Note d'implémentation, pas un écart :** le flou du portage est une passe plein écran entre le monde
et le GUI (`ZS/ZioScreen.java:352`), là où zio appelle `MaskShaderRenderer.renderBlur` au milieu de sa
propre passe (`ClientScreen.java:506`). Le rectangle est donc le même dans la même image, mais la
matière floutée derrière le tooltip est la scène, là où zio floute le panneau déjà dessiné dessous.
C'est la conséquence de l'architecture à deux passes du portage, et cela ne se juge qu'en jeu.

## 10. Vérification

```
$ ./gradlew clean build
BUILD SUCCESSFUL
tests=333  failures+errors=0  warnings=0
```

Répartition par fichier de test, lue dans `build/test-results/test/TEST-*.xml` — suite du portage
(`com.pro0101.vaultline.gui.zio`, 38 fichiers, **302 tests**) :

```
ZioAnimTest 7            ZioBindModeTest 3        ZioBindModesTest 7
ZioChromeTest 6          ZioColorMarkerTest 9     ZioCursorRuleTest 5
ZioDragTest 9            ZioEnumBoxTest 8         ZioFontAtlasTest 12
ZioFontConfigTest 5      ZioGamePanelTest 5       ZioGuiTextSourceTest 3
ZioHudEditorFxTest 5     ZioHudLayoutTest 12      ZioHudMarginStoreTest 6
ZioHudMarginsTest 8      ZioHudSizeStoreTest 8    ZioInputRouterTest 15
ZioListLayoutTest 16     ZioMinesweeperTest 15    ZioOpenRowsTest 5
ZioPaintTest 15          ZioPaintEffectsTest 5    ZioPanelPaintTest 6
ZioPresetsTest 9         ZioRowFadeTest 5         ZioScaleTest 9
ZioScaleStoreTest 7      ZioScissorTest 8         ZioScrollTest 7
ZioSnakeTest 10          ZioSnowTest 7            ZioTetrisTest 17
ZioTextMetricsTest 6     ZioGamesTest 5           ZioSecretSequenceTest 6
ZioSnowflakeTest 6       ZioTooltipFrameTest 5
                                                     sous-total 302
```

Le reste du dépôt porte les tests des autres modules, hors GUI (19 fichiers, **56 tests**) :
`BoostThrustTest` 6, `BoostEnvelopeTest` 7, `HeightBandTest` 6, `ChestLabelerScriptTest` 1,
`ChestPlacerScriptTest` 2, `InventoryDumperScriptTest` 1, `ItemGroupingKindTest` 6,
`ItemGroupingMaterialTest` 3, `ItemSorterScriptTest` 1, `ShulkerNamesTest` 3,
`ShulkerPackerScriptTest` 1, `ShulkerUnpackerScriptTest` 1, `SlotLayoutTest` 3, `StashPlanTest` 2,
`StashPlannerTest` 4, `StashScannerScriptTest` 1, `StashSessionStoreTest` 2, `StepScriptTest` 3,
`TimingsTest` 3.

```
302 (GUI) + 56 (autres modules) = 358
```

Les cinq effets d'un portage précédent ont ajouté 21 tests (`ZioColorMarkerTest` 9, `ZioRowFadeTest` 5,
`ZioHudEditorFxTest` 5, `ZioPaintEffectsTest` 2), la séquence secrète et le flocon 17
(`ZioSecretSequenceTest` 6, `ZioGamesTest` 5, `ZioSnowflakeTest` 6), et le rectangle partagé du tooltip 3
(`ZioPaintEffectsTest` passe de 2 à 5) et son ordre de décision par image 5 (`ZioTooltipFrameTest`) ; la
suite GUI en comptait donc 256 avant, soit **256 + 56 = 312 tests préexistants** et **46 tests neufs**,
pour **358**.

Un test préexistant a dû être réécrit, et un seul : `ZioPaintTest`.
`aSnowflakeIsDrawnHalfTheScreenDeepAndFadesAsItFalls` vérifiait le glyphe tapé, qui n'existe plus ; il
décrit maintenant les mêmes trois flocons à travers les pixels soumis, avec les mêmes positions et les
mêmes alphas. `ZioPaintTest` reste à 15 tests.

**Limite de preuve, établie par git :**

```
$ git ls-files src/test | wc -l                                    → 21
$ git ls-files src/test/java/com/pro0101/vaultline | wc -l          → 0
$ git ls-files --others --exclude-standard src/test/java/com/pro0101/vaultline | wc -l → 56
```

Les sources de test de ce portage ne sont pas suivies par git (aucun fichier sous
`src/test/java/com/pro0101/vaultline` n'est indexé ; les 21 fichiers indexés sont les anciens chemins
`stashorganizer`, supprimés de la copie de travail et remplacés par les fichiers `vaultline`
correspondants, non indexés). Un `git diff` ne peut donc pas prouver l'antériorité des 312 tests :
c'est une limite de preuve reconnue, et la preuve retenue est le décompte de méthodes par fichier
ci-dessus, plus le fait que `ZioPaintTest` est revenu à ses 15 tests, ses deux derniers ajouts vivant
dans `ZioPaintEffectsTest`.
