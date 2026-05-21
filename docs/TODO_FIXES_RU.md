# План доработок и фиксов (Swifty Proteins)

Чеклист актуальных улучшений по кодовой базе. Отмечайте `[x]` по мере выполнения.

**Связано:** [HOW_IT_WORKS_RU.md](HOW_IT_WORKS_RU.md) · [ARCHITECTURE_OVERVIEW.md](ARCHITECTURE_OVERVIEW.md)

---

## Приоритет 2 — баги и консистентность

- [ ] **8. Кнопка Retry при ошибке загрузки**
  - **Файлы:** [`ProteinListScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinlist/ProteinListScreen.kt), [`ProteinViewScreen.kt`](../app/src/main/java/com/music42/swiftyprotein/ui/proteinview/ProteinViewScreen.kt)
  - **Шаг:** в `AlertDialog` добавить «Повторить» → повторный `fetchLigand` / `onLigandClick`.

- [ ] **9. Подтверждение Logout**
  - **Шаг:** `AlertDialog` «Выйти?» перед `sessionViewModel.logout()` + navigate Login.
  - **Место:** общий callback в NavGraph или отдельный composable-хелпер.

---

## Приоритет 3 — поддержка и качество

- [ ] **10. Убрать дубликат `ligands.txt`**
  - **Сейчас:** [`ligands.txt`](../ligands.txt) (корень) и [`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt) — одинаковые 1243 строки.
  - **Шаг:** оставить один источник (`res/raw`); удалить корневый или генерировать в Gradle из одного файла.

---

## Приоритет 4 — полировка (по желанию)

- [ ] **13. Settings: повторить onboarding**
  - Сброс `onboardingCompleted` в DataStore + переход на `OnboardingScreen`.

- [ ] **14. Settings: очистить кэш CIF**
  - Удаление `filesDir/cif_cache/`, показ размера, сброс `cachedInfo` в списке.

- [ ] **15. Snackbar при toggle избранного**
  - Вместо тихого переключения — короткое «Добавлено» / «Удалено из избранного».

---

## Быстрая сводка

| P | # | Суть | Оценка усилий |
|---|-----|------|----------------|
| 2 | 8–9 | Retry, подтверждение logout | Мало |
| 3 | 10 | Дубликат ligands.txt | Мало |
| 4 | 13–15 | UX-полировка | По желанию |
