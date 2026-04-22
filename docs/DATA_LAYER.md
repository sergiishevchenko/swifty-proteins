# Data Layer (Ligands, Network, Cache)

## Goal

Load ligand IDs from a bundled list, download CIF files from RCSB, cache them, and parse them into the domain model used by the 3D viewer.

## Key files

- `app/src/main/res/raw/ligands.txt`
- `app/src/main/java/com/music42/swiftyprotein/data/remote/RcsbApi.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/repository/LigandRepository.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/parser/CifParser.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/model/Ligand.kt`

## Ligand IDs source

The catalog is loaded from `res/raw/ligands.txt`.

## Network endpoint

The app fetches CIF from:

- `https://files.rcsb.org/ligands/view/{ID}.cif`

Implemented via Retrofit in `RcsbApi`.

## Caching strategy

Downloaded CIF files are cached to disk under:

- `filesDir/cif_cache/<LIGAND_ID>.cif`

On the next load:

1. If the cache file exists, it is read first.
2. If parsing succeeds (atoms present), the cached version is used.
3. If parsing fails, the repository falls back to a fresh download.

## Error handling (high level)

The repository maps common failures to user-facing messages:

- DNS/host resolution failures
- timeouts
- HTTP errors (including 404 for missing ligand)
- parse failures / empty bodies
