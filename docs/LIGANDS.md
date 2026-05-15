# Ligands Documentation

## Data Source Used by the App

- Current endpoint pattern: `https://files.rcsb.org/ligands/view/{ID}.cif`
- Example: [HEM.cif (view)](https://files.rcsb.org/ligands/view/HEM.cif)

`{ID}` here is a **ligand/chemical component ID** (CCD ID), for example `HEM`, `ATP`, `NAG`, or numeric-like IDs such as `001` from [`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt).

---

## The Core Reason: ligands.txt Contains Ligand IDs, Not PDB Entry IDs

There are two different identifier systems:

- **Ligand/CCD ID**: usually 3 characters (examples: `HEM`, `ATP`, `001`)
- **PDB entry ID**: 4 characters (example: `4hhb`, `1crn`)

[`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt) contains the first type (ligand IDs).  
But `.pdb` download URLs on RCSB are for the second type (full structure entries).

So this does **not** work conceptually:

- `ligand ID` -> `/download/{id}.pdb`

because `{id}` is not a valid PDB entry ID.

---

## Why You Cannot Reliably Use `.pdb` with ligands.txt

### 1) Endpoint mismatch

RCSB ligand CIF endpoints (ligand ID domain) include:

- `/ligands/view/{ID}.cif` — used by this app and the subject (example: [HEM.cif](https://files.rcsb.org/ligands/view/HEM.cif))
- `/ligands/download/{ID}.cif` — alternate RCSB path for the same resource (example: [HEM.cif](https://files.rcsb.org/ligands/download/HEM.cif))

RCSB legacy PDB endpoint is for entries:

- `/download/{ENTRY}.pdb` (example: [4hhb.pdb](https://files.rcsb.org/download/4hhb.pdb))

These are different resources with different ID domains.

### 2) No 1:1 mapping

A single ligand ID (for example `ATP`) can appear in many different PDB entries.  
Without choosing a specific entry, chain, and residue instance, there is no unique `.pdb` file to download for "the ATP ligand".

### 3) Assignment input drives format choice

If input is [`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt) (ligand IDs), the technically correct direct download from RCSB is ligand CIF, because that endpoint is keyed by ligand IDs.

### 4) Parsing quality

For isolated ligand definitions (atoms + bonds + bond order), CIF is structured and explicit for this use-case.

---

## `.cif` vs `.pdb` — Practical Difference

| Aspect | `.pdb` (legacy) | `.cif` / mmCIF (modern) |
|---|---|---|
| Data model | Fixed-width text columns | Named fields and table-like `loop_` sections |
| Parsing | Position-sensitive | Column/key-driven |
| Extensibility | Limited | High |
| Typical modern usage | Full structure compatibility/legacy workflows | Current wwPDB preferred structured format |
| Ligand CCD direct download on RCSB | Not the standard ligand endpoint | Standard ligand endpoint |

---

## What Ligand CIF Looks Like

Real-world shape of ligand CIF data:

```text
data_HEM
#
_chem_comp.id                                    HEM
_chem_comp.name                                  "PROTOPORPHYRIN IX CONTAINING FE"
_chem_comp.formula                               "C34 H32 Fe N4 O4"
...
loop_
_chem_comp_atom.comp_id
_chem_comp_atom.atom_id
_chem_comp_atom.type_symbol
_chem_comp_atom.model_Cartn_x
_chem_comp_atom.model_Cartn_y
_chem_comp_atom.model_Cartn_z
...
HEM CHA  C  ... 2.748  -19.531 39.896 ...
HEM FE   FE ... 2.196  -20.749 36.814 ...
```

Key sections used by this app:

- `_chem_comp.name` -> ligand name
- `_chem_comp_atom.*` -> atom IDs/elements/coordinates
- `_chem_comp_bond.*` -> connectivity and bond order

---

## What PDB Looks Like (for full entries)

Example of legacy PDB line format:

```text
HEADER    OXYGEN TRANSPORT                         07-MAR-84   4HHB
ATOM      1  N   VAL A   1      11.104  14.099   2.100  1.00 49.05           N
ATOM      2  CA  VAL A   1      12.560  14.250   2.300  1.00 43.27           C
HETATM 1234  FE  HEM A 142      10.500  12.300   8.100  1.00 20.00          FE
CONECT 1234 1235 1236
END
```

Important: this file is tied to a **specific entry** (here `4HHB`) and a **specific instance** of a ligand inside that entry.

---

## If Someone Forces a Strict ".pdb only" Interpretation

To use `.pdb` strictly, project input must change from ligand IDs to entry IDs, for example:

1. replace [`app/src/main/res/raw/ligands.txt`](../app/src/main/res/raw/ligands.txt) with a list of 4-char PDB entry IDs
2. download `https://files.rcsb.org/download/{entry}.pdb` (example: [4hhb.pdb](https://files.rcsb.org/download/4hhb.pdb))
3. parse `HETATM`/`CONECT`
4. choose specific ligand occurrences inside each entry

This is a different data model and a different product scope than "list ligand IDs and render each ligand definition".

---

## References

- [RCSB File Download Services](https://www.rcsb.org/docs/programmatic-access/file-download-services)
- RCSB ligands view pattern used in this app: `https://files.rcsb.org/ligands/view/{ID}.cif` — example [HEM.cif](https://files.rcsb.org/ligands/view/HEM.cif)
