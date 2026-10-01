# Versi 1 — Analisis White-Box Persamaan Kuadrat

Bentuk dasar: ax² + bx + c = 0. Diskriminan D = b² − 4ac.

Decision: a==0, b==0, c==0, D<0, D==0. Jumlah decision = 5.

V(G) = D + 1 = 5 + 1 = 6.

| Path | Input | Expected |
|---|---|---|
| P1 | (0,0,0) | Tak hingga banyak solusi |
| P2 | (0,0,1) | Tidak ada solusi |
| P3 | (0,2,-4) | Persamaan linear, x=2 |
| P4 | (1,0,1) | Tidak ada akar real |
| P5 | (1,-2,1) | Akar kembar, x=1 |
| P6 | (1,-3,2) | Dua akar real, x1=2 dan x2=1 |

P1–P6 mencakup seluruh statement yang dapat dijangkau sehingga target SC 100%. Kelima decision memiliki outcome true/false yang feasible sehingga target BC 100%.

Method solve tidak mempunyai loop eksplisit, sehingga LC tidak diterapkan. Jika ditambah menu while, uji 0, 1, dan >1 iterasi.
