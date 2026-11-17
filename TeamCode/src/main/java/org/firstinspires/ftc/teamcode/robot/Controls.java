package org.firstinspires.ftc.teamcode.robot;

import com.qualcomm.robotcore.hardware.Gamepad;

public class Controls {
    private final Gamepad driver, operator;
    public Controls(Gamepad driver, Gamepad operator) { // da i parametri del gamepad1 e gamepad2 alla classe Controls tramite Gamepad
        this.driver = driver; // verrà poi inizializzato gamepad1, normalmente
        this.operator = operator; // verrà poi inizializzato gamepad2, normalmente
    }

    private static final double DEADZONE = 0.05; // per lo stick drift
    private static final double SLOW_FACTOR = 0.4; // fattore di rallentamento della velocità
    private static final double PRECISION_FACTOR = 2; // fattore di precisione della velocità perché la elevo al PRECISION_FACTOR

    // Movimento
    public double strafe()  { return shape(driver.left_stick_x); } // Movimento laterale dx e sx
    public double forward() { return shape(-driver.left_stick_y); } // Movimento avanti e indietro
    public double turn()    { return shape(driver.right_stick_x); } // Rotazione dx e sx
    public double driveScale() { return driver.right_bumper ? SLOW_FACTOR : 1.0; } // Fattore di rallentamento se mm
    public boolean isSlowMode() { return driver.right_bumper; } // Da vero se modalità rallentamento attiva
    public boolean isChasingBall() { return driver.left_bumper; } // Da vero se modalità seguimento palla attiva

    private double shape(double v){ // annulla lo stick drift ed eleva al cubo la velocità per maggiore precisione alle basse velocità
        if (Math.abs(v)< DEADZONE) return 0.0;
        return Math.pow(v, PRECISION_FACTOR);
    }
}

