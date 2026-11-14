package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.vision.BallData;
import org.firstinspires.ftc.teamcode.vision.Vision;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import java.util.List;

public class Robot {
    public final Drivetrain drivetrain;
    public final Vision vision;
    private final List<LynxModule> hubs;

    // Aggiorno le letture del colore
    public BallData activeBall = new BallData();
    public int activeColor = Vision.COLOR_RED_PIPE;

    /// *---* Funzione che richiama tutte le inizializzazioni *---*
    public Robot(HardwareMap hw) {

        // in questo caso tutti i pacchetti dal control hub vengono mandati tutti insieme così è più veloce
        hubs = hw.getAll(LynxModule.class);
        for (LynxModule hub : hubs)
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL); // è da aggiornare ogni ciclo, se non vuoi farlo mettilo in AUTO

        drivetrain = new Drivetrain(hw); // Richiamano Drivetrain e Vision per inizializzare i dispositivi dando alle classi i parametri hardwareMap
        vision = new Vision(hw);

    }

    public void start()  { vision.start(); }

    /// *---* Funzione per aggiornare i valori *---*
    // Aggiorna i dati restituiti dall'hub e anche la pipeline del colore
    public void update() {
        for (LynxModule hub : hubs) hub.clearBulkCache();

        // aggiorno ad ogni ciclo il colore rilevato dalla limelight
        if (vision.isAvailable()){
            if (activeColor==Vision.COLOR_RED_PIPE){
                activeColor = Vision.COLOR_RED_PIPE;
            }
            else if (activeColor == Vision.COLOR_BLUE_PIPE) {
                activeColor = Vision.COLOR_BLUE_PIPE;
            }
            else if (activeColor == Vision.COLOR_YELLOW_PIPE) {
                activeColor = Vision.COLOR_YELLOW_PIPE;
            }
        }
    }

    public void stop() {
        drivetrain.stopDrive();
        vision.stop();
    }
}