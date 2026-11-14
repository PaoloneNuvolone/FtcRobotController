package org.firstinspires.ftc.teamcode.vision;

public class BallData {
    public final double tx;       // angolo orizzontale (gradi)
    public final double ta;       // area (% del frame)
    public final double distance; // distanza stimata (cm)
    public final boolean valid;   // true se c'è un target valido


    // Costruttore per quando getBallData (Vision.java) restituisce un risultato valido
    public BallData(double tx, double ta, double distance) {
        this.tx = tx;
        this.ta = ta;
        this.distance = distance;
        this.valid = true;
    }

    // costruttore per "nessun risultato"
    public BallData() {
        this.tx = 0;
        this.ta = 0;
        this.distance = 0;
        this.valid = false;
    }
}
