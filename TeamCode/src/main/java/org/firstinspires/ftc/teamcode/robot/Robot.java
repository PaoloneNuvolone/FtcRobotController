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

}