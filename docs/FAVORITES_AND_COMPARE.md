# Favorites & Compare

## Favorites

### Goal

Allow users to save ligand IDs locally and revisit them quickly.

### Key files

- `app/src/main/java/com/music42/swiftyprotein/ui/favorites/FavoritesScreen.kt`
- `app/src/main/java/com/music42/swiftyprotein/ui/favorites/FavoritesViewModel.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/repository/FavoritesRepository.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/local/FavoritesDao.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/local/entity/FavoriteLigand.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/local/AppDatabase.kt`

### Storage

Favorites are stored in **Room (SQLite)** as ligand IDs.

## Compare

### Goal

Show two ligands side-by-side in a simplified viewer so the user can visually compare them.

### Key files

- `app/src/main/java/com/music42/swiftyprotein/ui/compare/CompareScreen.kt`
- `app/src/main/java/com/music42/swiftyprotein/ui/compare/CompareViewModel.kt`

### Behavior

- Both ligands are loaded via `LigandRepository`.
- Each panel renders its own SceneView scene.
- Controls:
  - orbit/rotation
  - pinch zoom
  - zoom +/- buttons per panel
