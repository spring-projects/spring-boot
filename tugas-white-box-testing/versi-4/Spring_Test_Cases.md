# Versi 4 — Test Case Tambahan Spring Owner.getPet()

| TC | Daftar Pet | name | ignoreNew | Expected | Loop |
|---|---|---|---|---|---|
| S01 | [] | Rex | false | null | 0 |
| S02 | [Rex(old)] | Milo | false | null | 1 |
| S03 | [Rex(old)] | Rex | false | Rex | 1 |
| S04 | [Rex(new)] | Rex | true | null | 1, filter false |
| S05 | [Rex(new), Milo(old)] | Milo | true | Milo | >1 |
| S06 | [Rex(old), Milo(old)] | Milo | false | Milo | match iterasi 2 |
| S07 | [Rex(old), Milo(old), Nia(old)] | Zoe | false | null | >1, no match |
| S08 | [Rex(new), Milo(new)] | Milo | true | null | >1, semua tersaring |

Decision coverage: for T/F, filter T/F, name comparison T/F. Loop categories: 0, 1, >1, no-match exit, dan early-match exit.