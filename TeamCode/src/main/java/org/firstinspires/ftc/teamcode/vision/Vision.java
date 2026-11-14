package org.firstinspires.ftc.teamcode.vision;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Vision {


    /// ---* LIMELIGHT *---
    /// Posizione: davanti al robot

    public static int COLOR_RED_PIPE = 1;
    public static int COLOR_BLUE_PIPE = 2;
    public static int COLOR_YELLOW_PIPE = 3;

    // Costante per il calcolo della distanza:
    // distanza(cm) = DISTANCE_K / sqrt(ta)
    // Calibra DISTANCE_K con misure reali (es. a 50 cm misura ta, poi K = 50 * sqrt(ta))
    private static final double DISTANCE_K = 120.0;
    private int currentPipeLine = 1;

    private Limelight3A limelight;

    public Vision(HardwareMap hw) {
        try { // tryGet ritorna null se il dispositivo non è configurato
            limelight = hw.tryGet(Limelight3A.class, "limelight");
            if (limelight != null) {
                limelight.setPollRateHz(100);
                pipeLineSwitch(1); // 1 = red, 2 = blue, 3 = yellow
            }
        } catch (Exception e) {
            limelight = null;
        }
    }

    // vero solo se la limelight esiste ed è effettivamente connessa
    public boolean isAvailable() {
        try {
            return limelight != null && limelight.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    // funzioni per interfacciarsi con limelight
    public void start() {
        if (limelight == null) return;
        try { limelight.start(); } catch (Exception ignored) {}
    }


    public void stop() {
        if (limelight == null) return;
        try { limelight.stop(); } catch (Exception ignored) {}
    }

    // Dato che abbiamo più pipeline per colore, abbiamo una funzione per cambiare pipeline,
    // inoltre aggiorna anche currentPipeline, questo serve per la funzione getBallData, per non
    // far aggiornare ad ogni ciclo il valore della pipeline corrente.
    public void pipeLineSwitch(int i) {
        if (limelight == null) return;
        else {
            limelight.pipelineSwitch(i);
            currentPipeLine = i;
        }
    }

    // risultato grezzo della limelight
    public LLResult getLatestResult() {
        if (limelight == null) return null;
        try {
            return limelight.getLatestResult();
        } catch (Exception e) {
            return null;
        }
    }


    // --- Metodo generico privato ---
    // restituisce angolo x, area e distanza, calcolata con DISTANCE_K (da calibrare)
    private BallData getBallData(int pipeline) {
        if (!isAvailable()) return new BallData();

        if (pipeline!=currentPipeLine){
            pipeLineSwitch(pipeline); // solo se si cambia pipeline si entra nell'IF
            try { Thread.sleep(50); } catch (InterruptedException ignored) {} // aspetta solo qui
        }

        LLResult result = getLatestResult();
        if (result == null || !result.isValid()) return new BallData();

        double tx = result.getTx();   // angolo orizzontale (-29.8° ... +29.8°)
        double ta = result.getTa();   // area (0% ... 100%)

        if (ta <= 0) return new BallData(); // evita divisione per zero

        double distance = DISTANCE_K / Math.sqrt(ta); // Calcolo per la distanza

        return new BallData(tx, ta, distance); // resituisce tx, ta e distance e li manda alla classe BallData.java
    }

    // --- API pubblica ---
    // Modo per cambiare colore da seguire in modo intelligente

    public BallData redData() {
        return getBallData(COLOR_RED_PIPE);
    }

    public BallData blueData() {
        return getBallData(COLOR_BLUE_PIPE);
    }

    public BallData yellowData() {
        return getBallData(COLOR_YELLOW_PIPE);
    }


    /// ---* LOGITECH *---
    /// posizione: sulla torretta


}