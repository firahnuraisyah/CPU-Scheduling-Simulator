import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;

public class CPUschedullingRR_PP {

    // =================================================================
    //  1. MODEL DATA
    // =================================================================

    static class Proses {
        final String id;
        final int arrival;    // Arrival Time
        final int burst;      // Burst Time
        final int priority;   // Prioritas (kecil = penting)

        int sisa;                 // sisa burst yang belum dieksekusi
        int mulaiPertama = -1;    // saat pertama kali mendapat CPU
        int selesai;              // Completion Time
        int waiting, turnaround, response;

        Proses(String id, int arrival, int burst, int priority) {
            this.id = id;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.sisa = burst;
        }

        // Salinan bersih supaya data input asli tidak berubah oleh simulasi. 
        Proses salin() {
            return new Proses(id, arrival, burst, priority);
        }
    }

    // Satu blok pada Gantt chart: proses "id" memakai CPU pada [mulai, selesai). 
    static class Segmen {
        final String id;
        final int mulai;
        int selesai;

        Segmen(String id, int mulai, int selesai) {
            this.id = id;
            this.mulai = mulai;
            this.selesai = selesai;
        }
    }

    // Hasil lengkap satu kali simulasi sebuah algoritma. 
    static class Hasil {
        final String algoritma;
        final List<Proses> proses = new ArrayList<>();
        final List<Segmen> gantt = new ArrayList<>();
        double avgWT, avgTAT, avgRT, cpuUtil, throughput;
        int maxWT;   // waiting time terpanjang -> indikator keadilan / starvation
        int ctxSwitch, totalWaktu;

        Hasil(String algoritma) {
            this.algoritma = algoritma;
        }
    }

    static final String IDLE = "IDLE";

    // =================================================================
    //  2. ALGORITMA PENJADWALAN
    // =================================================================

    /**
      Round Robin dengan time quantum tetap.
     
      Langkah: 
      1. masukkan proses yang sudah tiba ke antrian
      2. ambil proses terdepan, jalankan min(quantum, sisa)
      3. proses yang tibanselama eksekusi masuk antrian
      4. jika belum selesai, proses kembali ke belakang antrian.
     */
    static Hasil roundRobin(List<Proses> input, int quantum) {
        Hasil h = new Hasil("Round Robin (q=" + quantum + ")");
        List<Proses> p = salinTerurut(input, h);
        // [BEDA-RR 1] STRUKTUR DATA: RR memakai antrian FIFO; urutan masuk = urutan dilayani.
        Queue<Proses> antrian = new ArrayDeque<>();
        int n = p.size(), waktu = 0, indeks = 0, selesai = 0;

        while (selesai < n) {
            // 1. proses yang sudah tiba pada 'waktu' masuk antrian
            while (indeks < n && p.get(indeks).arrival <= waktu) {
                antrian.add(p.get(indeks++));
            }
            // CPU menganggur: lompat ke kedatangan berikutnya
            if (antrian.isEmpty()) {
                int tiba = p.get(indeks).arrival;
                tambahSegmen(h.gantt, IDLE, waktu, tiba);
                waktu = tiba;
                continue;
            }
            // 2. eksekusi proses terdepan selama maksimal satu quantum
            // [BEDA-RR 2] MEMILIH PROSES: ambil yang paling depan antrian. Nilai prioritas TIDAK dipakai.
            Proses cur = antrian.poll();
            if (cur.mulaiPertama < 0) cur.mulaiPertama = waktu;
            // [BEDA-RR 3] LANGKAH WAKTU: maju min(quantum, sisa). Preemption terjadi karena WAKTU (quantum) HABIS.
            int jalan = Math.min(quantum, cur.sisa);
            tambahSegmen(h.gantt, cur.id, waktu, waktu + jalan);
            waktu += jalan;
            cur.sisa -= jalan;

            // 3. kedatangan selama eksekusi masuk lebih dulu ...
            while (indeks < n && p.get(indeks).arrival <= waktu) {
                antrian.add(p.get(indeks++));
            }
            // 4. ... kemudian proses yang di-preempt kembali ke ekor antrian
            // [BEDA-RR 4] proses yang belum selesai DIMASUKKAN LAGI ke ekor antrian (bergiliran).
            if (cur.sisa > 0) {
                antrian.add(cur);
            } else {
                cur.selesai = waktu;
                selesai++;
            }
        }
        hitungMetrik(h);
        return h;
    }

    /**
      Priority Preemptive (angka kecil = prioritas tinggi).
     
      Setiap 1 unit waktu, dari semua proses yang sudah tiba dan belum selesai dipilih proses berprioritas tertinggi. 
      Proses yang baru tiba dengan prioritas lebih tinggi otomatis merebut CPU (preemption).
      Prioritas sama -> yang lebih dulu datang (FCFS).
     */
    static Hasil priorityPreemptive(List<Proses> input) {
        Hasil h = new Hasil("Priority Preemptive");
        List<Proses> p = salinTerurut(input, h);
        int n = p.size(), waktu = 0, selesai = 0;

        while (selesai < n) {
            // [BEDA-PP 1] STRUKTUR DATA: tidak ada antrian; tiap 1 unit waktu SEMUA proses yang sudah tiba dipindai ulang.
            // [BEDA-PP 2] MEMILIH PROSES: angka prioritas terkecil; jika seri, yang datang lebih dulu (FCFS). Quantum tidak ada.
            Proses terpilih = null;
            for (Proses x : p) {
                if (x.arrival <= waktu && x.sisa > 0) {
                    if (terpilih == null
                            || x.priority < terpilih.priority
                            || (x.priority == terpilih.priority && x.arrival < terpilih.arrival)) {
                        terpilih = x;
                    }
                }
            }
            // tidak ada proses siap -> CPU idle 1 unit
            if (terpilih == null) {
                tambahSegmen(h.gantt, IDLE, waktu, waktu + 1);
                waktu++;
                continue;
            }
            if (terpilih.mulaiPertama < 0) terpilih.mulaiPertama = waktu;
            // [BEDA-PP 3] LANGKAH WAKTU: selalu 1 unit. Preemption terjadi karena proses berprioritas LEBIH TINGGI
            //             tiba, sebab keputusan dihitung ulang setiap unit.
            // [BEDA-PP 4] tidak ada antrian ulang: proses yang direbut CPU-nya hanya berhenti dan ikut dipindai lagi.
            tambahSegmen(h.gantt, terpilih.id, waktu, waktu + 1);
            waktu++;
            terpilih.sisa--;
            if (terpilih.sisa == 0) {
                terpilih.selesai = waktu;
                selesai++;
            }
        }
        hitungMetrik(h);
        return h;
    }

    // =================================================================
    //  3. FUNGSI BANTU SIMULASI
    // =================================================================

    // Menyalin proses, mengurutkan berdasarkan Arrival Time (stabil). 
    static List<Proses> salinTerurut(List<Proses> input, Hasil h) {
        for (Proses x : input) h.proses.add(x.salin());
        List<Proses> urut = new ArrayList<>(h.proses);
        urut.sort((a, b) -> Integer.compare(a.arrival, b.arrival));
        return urut;
    }

    // Menambah blok Gantt; blok berurutan milik proses yang sama digabung. 
    static void tambahSegmen(List<Segmen> gantt, String id, int mulai, int selesai) {
        if (!gantt.isEmpty()) {
            Segmen akhir = gantt.get(gantt.size() - 1);
            if (akhir.id.equals(id) && akhir.selesai == mulai) {
                akhir.selesai = selesai;
                return;
            }
        }
        gantt.add(new Segmen(id, mulai, selesai));
    }

    /**
      Menghitung semua metrik dari Completion Time:
        TAT = CT - AT      WT = TAT - BT      RT = start pertama - AT
      Context switch = berpindahnya CPU dari satu proses ke proses LAIN
      (periode IDLE diabaikan).
     */
    static void hitungMetrik(Hasil h) {
        double sumWT = 0, sumTAT = 0, sumRT = 0;
        int n = h.proses.size(), busy = 0, akhir = 0;
        for (Proses x : h.proses) {
            x.turnaround = x.selesai - x.arrival;
            x.waiting = x.turnaround - x.burst;
            x.response = x.mulaiPertama - x.arrival;
            sumWT += x.waiting;
            sumTAT += x.turnaround;
            sumRT += x.response;
            busy += x.burst;
            akhir = Math.max(akhir, x.selesai);
            h.maxWT = Math.max(h.maxWT, x.waiting);
        }
        h.avgWT = sumWT / n;
        h.avgTAT = sumTAT / n;
        h.avgRT = sumRT / n;
        h.totalWaktu = akhir;
        h.cpuUtil = 100.0 * busy / akhir;
        h.throughput = (double) n / akhir;

        String terakhir = null;
        int ctx = 0;
        for (Segmen s : h.gantt) {
            if (s.id.equals(IDLE)) continue;
            if (terakhir != null && !terakhir.equals(s.id)) ctx++;
            terakhir = s.id;
        }
        h.ctxSwitch = ctx;
    }

    // =================================================================
    //  4. OUTPUT KONSOL
    // =================================================================

    static String f(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    // Gantt chart versi teks. 
    static void cetakGantt(Hasil h) {
        StringBuilder atas = new StringBuilder(), bawah = new StringBuilder();
        for (Segmen s : h.gantt) {
            int w = Math.max(s.id.length() + 2, 4);
            int kiriPad = (w - s.id.length()) / 2;
            atas.append('|').append(" ".repeat(kiriPad)).append(s.id)
                .append(" ".repeat(w - kiriPad - s.id.length()));
            bawah.append(String.format("%-" + (w + 1) + "d", s.mulai));
        }
        atas.append('|');
        bawah.append(h.gantt.get(h.gantt.size() - 1).selesai);
        System.out.println("  Gantt : " + atas);
        System.out.println("          " + bawah);
    }

    // Tabel metrik per proses + ringkasan. 
    static void cetakTabel(Hasil h) {
        System.out.println("\n--- " + h.algoritma + " ---");
        cetakGantt(h);
        System.out.println();
        System.out.println("  Proses  AT  BT  Prio  CT  TAT  WT  RT");
        for (Proses x : h.proses) {
            System.out.printf("  %-6s %3d %3d %5d %3d %4d %3d %3d%n",
                    x.id, x.arrival, x.burst, x.priority, x.selesai,
                    x.turnaround, x.waiting, x.response);
        }
        System.out.println("  Rata-rata WT  = " + f(h.avgWT));
        System.out.println("  Rata-rata TAT = " + f(h.avgTAT));
        System.out.println("  Rata-rata RT  = " + f(h.avgRT));
        System.out.println("  Waiting time terpanjang = " + h.maxWT);
        System.out.println("  Context switch = " + h.ctxSwitch
                + " | CPU util = " + f(h.cpuUtil) + "% | Throughput = "
                + String.format(Locale.US, "%.3f", h.throughput) + " proses/unit");
    }

    // =================================================================
    //  5. SKENARIO PENGUJIAN
    // =================================================================

    // Data dari jurnal (Tabel 2 & 4) -> untuk VALIDASI program. 
    static List<Proses> dataJurnal() {
        List<Proses> d = new ArrayList<>();
        d.add(new Proses("P1", 0, 7, 4));
        d.add(new Proses("P2", 3, 9, 2));
        d.add(new Proses("P3", 5, 5, 1));
        d.add(new Proses("P4", 9, 8, 3));
        d.add(new Proses("P5", 11, 12, 5));
        return d;
    }

    // Data buatan sendiri (6 proses; P4 dan P6 sengaja berprioritas sama). 
    static List<Proses> dataSendiri() {
        List<Proses> d = new ArrayList<>();
        d.add(new Proses("P1", 0, 10, 3));
        d.add(new Proses("P2", 2, 5, 1));
        d.add(new Proses("P3", 4, 8, 4));
        d.add(new Proses("P4", 6, 3, 2));
        d.add(new Proses("P5", 8, 6, 5));
        d.add(new Proses("P6", 10, 4, 2));
        return d;
    }

    /**
      Data sendiri versi C: proses yang SAMA dengan data B, tetapi prioritas
      dibalik sehingga proses panjang justru berprioritas tinggi. Dipakai untuk
      menguji apakah keunggulan Priority Preemptive bergantung pada data.
     */
    static List<Proses> dataPrioritasTerbalik() {
        List<Proses> d = new ArrayList<>();
        d.add(new Proses("P1", 0, 10, 1));
        d.add(new Proses("P2", 2, 5, 4));
        d.add(new Proses("P3", 4, 8, 2));
        d.add(new Proses("P4", 6, 3, 6));
        d.add(new Proses("P5", 8, 6, 3));
        d.add(new Proses("P6", 10, 4, 5));
        return d;
    }

    // Menjalankan kedua algoritma pada satu dataset, lalu mencetak hasilnya. 
    static Hasil[] jalankanSkenario(String judul, List<Proses> data, int quantum) {
        System.out.println("\n==================================================");
        System.out.println(" SKENARIO " + judul + "  (quantum RR = " + quantum + ")");
        System.out.println("==================================================");
        Hasil rr = roundRobin(data, quantum);
        Hasil pp = priorityPreemptive(data);
        cetakTabel(rr);
        cetakTabel(pp);
        return new Hasil[]{rr, pp};
    }

    // Uji sensitivitas: pengaruh besar quantum terhadap kinerja Round Robin.
    // Mengembalikan seluruh Hasil per-quantum supaya bisa dipakai ulang di kesimpulan akhir.
    static List<Hasil> analisisQuantum(List<Proses> data, int[] daftarQ) {
        System.out.println("\n==================================================");
        System.out.println(" SENSITIVITAS QUANTUM ROUND ROBIN (data sendiri)");
        System.out.println("==================================================");
        System.out.println("  q   AvgWT  AvgTAT  AvgRT  ContextSwitch");
        List<Hasil> hasilSemua = new ArrayList<>();
        for (int q : daftarQ) {
            Hasil h = roundRobin(data, q);
            hasilSemua.add(h);
            System.out.printf(Locale.US, "  %-3d %6.2f %7.2f %6.2f %8d%n",
                    q, h.avgWT, h.avgTAT, h.avgRT, h.ctxSwitch);
        }
        return hasilSemua;
    }

    // Validasi: hasil program harus sama dengan angka pada jurnal (Tabel 6). 
    static void validasiDenganJurnal(Hasil rr, Hasil pp) {
        System.out.println("\n>>> VALIDASI TERHADAP JURNAL (Widiarto dkk., 2024, Tabel 6)");
        cek("RR  Avg WT ", rr.avgWT, 14.6);
        cek("RR  Avg TAT", rr.avgTAT, 22.8);
        cek("PP  Avg WT ", pp.avgWT, 10.6);
        cek("PP  Avg TAT", pp.avgTAT, 18.8);
    }

    static void cek(String nama, double hasil, double harapan) {
        boolean ok = cocok(hasil, harapan);
        System.out.println("  " + nama + " : program = " + f(hasil) + " | jurnal = " + f(harapan)
                + (ok ? "  -> SESUAI" : "  -> BERBEDA"));
    }

    // Mode manual: pengguna memasukkan sendiri jumlah proses, AT, BT, prioritas, dan quantum. 
    static void modeManual() {
        Scanner in = new Scanner(System.in);
        System.out.print("Jumlah proses            : ");
        int n = in.nextInt();
        System.out.print("Time quantum Round Robin : ");
        int q = in.nextInt();
        List<Proses> data = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            System.out.print("Proses P" + i + " -> Arrival Burst Priority : ");
            data.add(new Proses("P" + i, in.nextInt(), in.nextInt(), in.nextInt()));
        }
        jalankanSkenario("MANUAL", data, q);
    }

    // =================================================================
    //  6. KESIMPULAN & VALIDASI KESELURUHAN (dihitung dari SEMUA data yang diuji)
    // =================================================================

    // Pembanding toleransi kecil untuk nilai desimal hasil rata-rata.
    static boolean cocok(double hasil, double harapan) {
        return Math.abs(hasil - harapan) < 1e-6;
    }

    /**
      Mencetak kesimpulan akhir program, dalam format ringkas:
      blok validasi (satu kali, di sini) + tabel perbandingan rata-rata + satu baris kesimpulan.
      Nilai rata-rata di tabel DIHITUNG dari seluruh objek Hasil skenario yang sudah dijalankan
      (A, B, C) -- bukan angka manual -- sehingga otomatis mengikuti data yang benar-benar diuji.
     */
    static void cetakKesimpulanAkhir(List<Hasil[]> semuaSkenario) {
        // ---- Validasi terhadap jurnal (hanya sekali, memakai skenario data jurnal) ----
        Hasil rrJurnal = semuaSkenario.get(0)[0];
        Hasil ppJurnal = semuaSkenario.get(0)[1];
        validasiDenganJurnal(rrJurnal, ppJurnal);

        // ---- Perbandingan rata-rata RR vs PP di seluruh skenario yang diuji ----
        int n = semuaSkenario.size();
        double rrWT = 0, rrTAT = 0, rrRT = 0, ppWT = 0, ppTAT = 0, ppRT = 0;
        double rrCtx = 0, ppCtx = 0, rrMaxWT = 0, ppMaxWT = 0;
        for (Hasil[] pasangan : semuaSkenario) {
            Hasil rr = pasangan[0], pp = pasangan[1];
            rrWT += rr.avgWT; rrTAT += rr.avgTAT; rrRT += rr.avgRT;
            rrCtx += rr.ctxSwitch; rrMaxWT += rr.maxWT;
            ppWT += pp.avgWT; ppTAT += pp.avgTAT; ppRT += pp.avgRT;
            ppCtx += pp.ctxSwitch; ppMaxWT += pp.maxWT;
        }
        rrWT /= n; rrTAT /= n; rrRT /= n; rrCtx /= n; rrMaxWT /= n;
        ppWT /= n; ppTAT /= n; ppRT /= n; ppCtx /= n; ppMaxWT /= n;

        System.out.println("\n--- PERBANDINGAN (rata-rata dari " + n + " skenario) ---");
        System.out.println("  Algoritma              Avg WT  Avg TAT  Avg RT  CtxSw  MaxWT");
        System.out.printf(Locale.US, "  %-22s %6.2f %8.2f %7.2f %6.1f %6.1f%n",
                "Round Robin", rrWT, rrTAT, rrRT, rrCtx, rrMaxWT);
        System.out.printf(Locale.US, "  %-22s %6.2f %8.2f %7.2f %6.1f %6.1f%n",
                "Priority Preemptive", ppWT, ppTAT, ppRT, ppCtx, ppMaxWT);

        String unggul = ppWT < rrWT ? "Priority Preemptive" : (rrWT < ppWT ? "Round Robin" : "Seri");
        System.out.println("  -> Waiting time rata-rata terendah: " + unggul);
    }

    // =================================================================
    //  7. PROGRAM UTAMA
    // =================================================================
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--manual")) {
            modeManual();
            return;
        }

        // Menyimpan seluruh Hasil dari tiap skenario agar bisa dipakai ulang
        // untuk kesimpulan keseluruhan di akhir program.
        List<Hasil[]> semuaSkenario = new ArrayList<>();

        // Skenario A: data jurnal (validasi implementasi), quantum = 4
        Hasil[] a = jalankanSkenario("A - DATA JURNAL", dataJurnal(), 4);
        semuaSkenario.add(a);

        // Skenario B: data buatan sendiri (6 proses), quantum = 3
        Hasil[] b = jalankanSkenario("B - DATA SENDIRI", dataSendiri(), 3);
        semuaSkenario.add(b);

        // Skenario C: proses sama dengan B, prioritas dibalik (proses panjang = prioritas tinggi)
        Hasil[] c = jalankanSkenario("C - PRIORITAS TERBALIK", dataPrioritasTerbalik(), 3);
        semuaSkenario.add(c);

        // Analisis tambahan: pengaruh quantum
        analisisQuantum(dataSendiri(), new int[]{1, 2, 3, 4, 6, 8, 10});

        // Kesimpulan & validasi keseluruhan (ringkas), tepat sebelum "Selesai."
        cetakKesimpulanAkhir(semuaSkenario);

        System.out.println("\nSelesai.");
    }
}