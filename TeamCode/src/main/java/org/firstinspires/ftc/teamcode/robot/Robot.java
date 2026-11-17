package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.vision.Vision;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import java.util.List;

public class Robot {
    public final Drivetrain drivetrain;
    public final Vision vision;
    private final List<LynxModule> hubs;

    // Variabili per chaseBall()
    public static double kP_turn = 0.018;             // più è grande l'angolo tx da correggere, più sarà la potenza powerTurn
    public static double kP_for = 0.004;              // kP ≈ (potenza desiderata - kS) / centimetri in cui voglio la potenza desiderata
    public static double deadbandGradi = 1;           // non corregge sotto questi gradi
    public static double deadbandDista = 2;           // non corregge sotto questi cm
    public static double kS_turn = 0.2;               // potenza minima data ai motori per ruotare
    public static double kS_for = 0.1;                // potenza minima data ai motori per avanzare o arretrare
    public static double MAX_TURN = 0.6;              // Velocità massima raggiungibile per la rotazione
    public static double MAX_FORWARD = 0.7;           // Velocità massima raggiungibile per la correzione in avanti o indietro
    public static double SOGLIA_ROTAZIONE = 5;        // Soglia di gradi sotto la quale prima ruota e basta poi si avanza o arretra

/// ------------------------------------------------------------------------------------------------

    public Robot(HardwareMap hw) {
        // in questo caso tutti i pacchetti dal control hub vengono mandati tutti insieme così è più veloce
        hubs = hw.getAll(LynxModule.class);
        for (LynxModule hub : hubs)
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL); // è da aggiornare ogni ciclo, se non vuoi farlo mettilo in AUTO

        vision = new Vision(hw);
        drivetrain = new Drivetrain(hw); // Richiamano Drivetrain e Vision per inizializzare i dispositivi dando alle classi i parametri hardwareMap
    }


    public void start()  { vision.start(); }


    public void update() {
        for (LynxModule hub : hubs) hub.clearBulkCache();
        vision.update();
    }


    public void stop() {
        drivetrain.stopDrive();
        vision.stop();
    }


    public void chaseBall(double targetDistance) {

        if ( !vision.isAvailable() ){
            drivetrain.drive (0,0,0);
            return;
        }

        // Calcoli per quanto ruotare
        double tx = vision.getTx();

        double powerTurn;
        if ( Math.abs(tx) > deadbandGradi){
            powerTurn = Math.signum(tx)*(kS_turn + kP_turn * Math.abs(tx));
            powerTurn = Math.max(-MAX_TURN,Math.min(MAX_TURN,powerTurn));
        } else {
            powerTurn = 0;
        }

        // Calcoli per quando avanzare o arretrare
        double distance = vision.getDistance();

        double powerFor;
        double errore = distance - targetDistance;
        if ( Math.abs(tx) > SOGLIA_ROTAZIONE){
            powerFor=0;
        }else {
            if ( errore > deadbandDista){
                powerFor = (kS_for + kP_for * errore);
                powerFor = Math.min( MAX_FORWARD , powerFor );
            } else if ( errore < -deadbandDista){
                powerFor = -(kS_for + kP_for * Math.abs(errore));
                powerFor = Math.max(-MAX_FORWARD,powerFor);
            } else {
                powerFor = 0;
            }
        }

        drivetrain.drive (0, powerFor, powerTurn);
    }
}