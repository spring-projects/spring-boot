# Versi 2 — Analisis Spring Petclinic REST

Project: spring-petclinic/spring-petclinic-rest.
File: src/main/java/org/springframework/samples/petclinic/model/Owner.java.
Package: org.springframework.samples.petclinic.model.
Class: Owner.
Method: getPet(String name, boolean ignoreNew).

Logika yang dianalisis: normalisasi name; for setiap Pet; if (!ignoreNew || !pet.isNew()); ambil dan normalisasi nama; if nama sama return pet; setelah loop return null.

CFG decision-level:
N1 normalisasi → N2 kondisi for.
N2 true → N3 filter new; N2 false → N7 return null.
N3 true → N4 ambil nama → N5 nama sama; N3 false → N2.
N5 true → N6 return pet; N5 false → N2.

Edges: N1-N2, N2(T)-N3, N2(F)-N7, N3(T)-N4, N3(F)-N2, N4-N5, N5(T)-N6, N5(F)-N2.

Decision = 3, sehingga V(G)=3+1=4. Basis path: P1 N1-N2(F)-N7; P2 N1-N2(T)-N3(F)-N2(F)-N7; P3 N1-N2(T)-N3(T)-N4-N5(F)-N2(F)-N7; P4 N1-N2(T)-N3(T)-N4-N5(T)-N6.

Branch coverage mencakup true/false untuk loop, filter new, dan perbandingan nama. Target BC 100% pada decision-level.

Loop coverage: 0 iterasi, 1 iterasi, >1 iterasi, keluar tanpa match, dan berhenti di tengah karena match.

| TC | Pets | name | ignoreNew | Expected |
|---|---|---|---|---|
| TC01 | [] | Rex | false | null |
| TC02 | [Rex] | Milo | false | null |
| TC03 | [Rex] | Rex | false | Rex |
| TC04 | [Rex(new)] | Rex | true | null |
| TC05 | [Rex(new), Milo(old)] | Milo | true | Milo |
| TC06 | [Rex(old), Milo(old)] | Milo | false | Milo |

TC01–TC06 dirancang untuk mencakup seluruh outcome decision-level serta kategori loop utama.
