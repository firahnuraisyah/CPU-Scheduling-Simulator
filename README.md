# CPU Scheduling Simulator

Program simulasi penjadwalan CPU menggunakan bahasa Java untuk membandingkan algoritma **Round Robin (RR)** dan **Priority Preemptive (PP)**. Program ini menampilkan Gantt Chart, menghitung metrik penjadwalan, serta melakukan validasi dan perbandingan hasil dari beberapa skenario pengujian.

Proyek ini dikembangkan sebagai bagian dari pembelajaran mata kuliah Sistem Operasi untuk memahami cara kerja algoritma penjadwalan proses pada CPU.

## Fitur

- **Round Robin:** Menjalankan proses berdasarkan antrian FIFO dengan *time quantum* yang dapat ditentukan.
- **Priority Preemptive:** Memilih proses dengan prioritas tertinggi berdasarkan nilai prioritas terkecil. Jika prioritas sama, proses yang datang lebih dahulu dipilih.
- **Gantt Chart:** Menampilkan urutan eksekusi proses dalam bentuk diagram berbasis teks.
- **Perhitungan metrik:** Menghitung Completion Time (CT), Turnaround Time (TAT), Waiting Time (WT), dan Response Time (RT).
- **Analisis kinerja:** Menampilkan rata-rata metrik, context switch, CPU utilization, throughput, dan waiting time terpanjang.
- **Pengujian beberapa skenario:** Menggunakan data dari jurnal, data buatan sendiri, serta skenario dengan prioritas terbalik.
- **Analisis time quantum:** Membandingkan hasil Round Robin dengan beberapa nilai quantum.
- **Validasi hasil:** Membandingkan rata-rata Waiting Time dan Turnaround Time dengan nilai acuan dari jurnal yang digunakan.

## Teknologi

- Java
- Java Collections Framework (`ArrayList`, `ArrayDeque`, `Queue`)
- Java Scanner untuk input pengguna
- Terminal atau Command Prompt

## Algoritma Penjadwalan

### 1. Round Robin (RR)

Round Robin menggunakan antrian FIFO dan memberikan jatah waktu eksekusi (*time quantum*) kepada setiap proses. Jika proses belum selesai setelah quantum habis, proses tersebut dimasukkan kembali ke belakang antrian. Proses yang baru tiba selama eksekusi dimasukkan ke antrian sebelum proses yang belum selesai dimasukkan kembali.

### 2. Priority Preemptive (PP)

Priority Preemptive memilih proses yang memiliki nilai prioritas terkecil di antara proses yang sudah tiba dan belum selesai. Pemilihan dievaluasi kembali setiap satu unit waktu sehingga proses dengan prioritas lebih tinggi dapat mengambil alih CPU. Jika prioritas sama, proses yang datang lebih dahulu dipilih.

## Metrik Pengukuran

| Metrik | Keterangan |
| Completion Time (CT) | Waktu ketika proses selesai dieksekusi |
| Turnaround Time (TAT) | Total waktu dari kedatangan hingga proses selesai |
| Waiting Time (WT) | Total waktu proses menunggu CPU |
| Response Time (RT) | Waktu dari kedatangan hingga proses pertama kali dieksekusi |
| Context Switch | Jumlah perpindahan eksekusi CPU dari satu proses ke proses lain |
| CPU Utilization | Persentase waktu CPU digunakan untuk menjalankan proses |
| Throughput | Jumlah proses yang selesai per unit waktu |

## Skenario Pengujian

Program menjalankan beberapa skenario secara otomatis:

1. **Data Jurnal:** Lima proses dengan time quantum 4 untuk validasi hasil.
2. **Data Sendiri:** Enam proses dengan time quantum 3 untuk membandingkan kinerja algoritma.
3. **Prioritas Terbalik:** Menggunakan proses yang sama seperti skenario kedua, tetapi nilai prioritas diubah.
4. **Analisis Quantum:** Menguji Round Robin dengan quantum 1, 2, 3, 4, 6, 8, dan 10.

Hasil dari ketiga skenario utama digunakan untuk membuat perbandingan rata-rata kinerja Round Robin dan Priority Preemptive.

## Cara Menjalankan Program

### Persyaratan

Pastikan Java Development Kit (JDK) sudah terpasang. Periksa instalasi melalui terminal:

```bash
java -version
javac -version
```

### 1. Clone repositori

```bash
git clone https://github.com/USERNAME/cpu-scheduling-simulator.git
cd cpu-scheduling-simulator
```

Ganti `USERNAME` dengan username GitHub pemilik repositori.

### 2. Kompilasi program

```bash
javac CPUschedullingRR_PP.java
```

### 3. Jalankan simulasi otomatis

```bash
java CPUschedullingRR_PP
```

Program akan menjalankan seluruh skenario pengujian, menampilkan Gantt Chart dan metrik masing-masing algoritma, kemudian menampilkan hasil validasi serta perbandingan keseluruhan.

### 4. Jalankan dengan input manual

```bash
java CPUschedullingRR_PP --manual
```

Pada mode manual, pengguna memasukkan jumlah proses, time quantum Round Robin, serta Arrival Time, Burst Time, dan Priority untuk setiap proses.

## Struktur Program

```text
cpu-scheduling-simulator/
├── CPUschedullingRR_PP.java
└── README.md
```

File Java berisi model data proses, implementasi algoritma penjadwalan, perhitungan metrik, skenario pengujian, dan program utama.

## Kontribusi Saya: Firah Nuraisyah

Kontribusi utama saya dalam proyek ini adalah **implementasi algoritma Round Robin**. Bagian yang saya kerjakan mencakup penggunaan antrian FIFO, pengaturan eksekusi berdasarkan time quantum, penanganan proses yang datang selama eksekusi, serta pengembalian proses yang belum selesai ke belakang antrian.

Saya juga mempelajari bagaimana hasil simulasi dapat digunakan untuk memahami dan mengevaluasi kinerja algoritma penjadwalan CPU.

## Tujuan Pembelajaran

- Memahami konsep penjadwalan proses pada sistem operasi.
- Mengimplementasikan algoritma Round Robin menggunakan Java.
- Memahami pengaruh time quantum terhadap hasil penjadwalan.
- Menganalisis metrik kinerja proses berdasarkan hasil simulasi.
- Membandingkan pendekatan penjadwalan berbasis quantum dan prioritas.

## Catatan

Proyek ini dibuat untuk tujuan pembelajaran dan simulasi algoritma penjadwalan CPU. Hasil perbandingan bergantung pada data proses dan parameter yang digunakan; tidak ada satu algoritma yang selalu menghasilkan metrik terbaik untuk semua skenario.
