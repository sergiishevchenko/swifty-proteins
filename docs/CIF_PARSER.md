# CIF Parser

## Goal

Convert an RCSB ligand CIF file into a lightweight in-app domain model:

- `Ligand` metadata (id/name/formula when available)
- `Atom` list (id, element, coordinates)
- `Bond` list (atom ids + order)

## Key files

- `app/src/main/java/com/music42/swiftyprotein/data/parser/CifParser.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/model/Ligand.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/model/Atom.kt`
- `app/src/main/java/com/music42/swiftyprotein/data/model/Bond.kt`

## Input assumptions

The parser targets RCSB ligand CIF format (chemical component dictionary style), using fields such as:

- `_chem_comp.id`, `_chem_comp.name`, `_chem_comp.formula`
- `loop_` tables:
  - `_chem_comp_atom.*` for atoms and 3D coordinates
  - `_chem_comp_bond.*` for bonds and bond order

## Output assumptions

- Coordinates are treated as Cartesian and used directly by the 3D scene builder.
- Bonds reference atoms by their CIF atom IDs.

## Failure handling

If parsing yields zero atoms, the repository treats this as a parse failure and may re-download (in case the cache is corrupted).
