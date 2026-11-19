package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Drivetrain {

    // Nomi dei dispositivi nella Robot Configuration
    private static final String FRONT_LEFT_MOTOR = "frontleftmotor";
    private static final String FRONT_RIGHT_MOTOR = "frontrightmotor";
    private static final String BACK_LEFT_MOTOR = "backleftmotor";
    private static final String BACK_RIGHT_MOTOR = "backrightmotor";

    // Motori della trasmissione
    private final DcMotor frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor;

    /// *---* Funzione per inizializzare i motori del movimento *---*
    public Drivetrain(HardwareMap hw) {
        if (hw == null) {throw new IllegalArgumentException("hwMap cannot be null");}

        // Recupero dei motori dalla HardwareMap
        frontLeftMotor = hw.get(DcMotor.class, FRONT_LEFT_MOTOR);
        frontRightMotor = hw.get(DcMotor.class, FRONT_RIGHT_MOTOR);
        backLeftMotor = hw.get(DcMotor.class, BACK_LEFT_MOTOR);
        backRightMotor = hw.get(DcMotor.class, BACK_RIGHT_MOTOR);

        // Inversione motori lato sinistro per far avanzare il robot con potenza positiva
        frontLeftMotor.setDirection(DcMotor.Direction.REVERSE);
        backLeftMotor.setDirection(DcMotor.Direction.REVERSE);
        frontRightMotor.setDirection(DcMotor.Direction.FORWARD);
        backRightMotor.setDirection(DcMotor.Direction.FORWARD);

        // Configurazione comportamento dei motori
        DcMotor[] motors = {frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor};
        for (DcMotor m : motors) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            m.setPower(0);
        }
    }

    /// *---* Funzione Di Guida col joystick *---*
    public void drive(double x, double y, double turn) {
        double lf = y + x + turn; // segna in che modo le ruote mechanum inluenzano la direzione del robot
        double rf = y - x - turn; // es: per andare dritto tutte e 4 influnzano --> perciò y positiva
        double lb = y - x + turn;
        double rb = y + x - turn;

        // Trova il valore assoluto massimo. Se tutti sono <= 1, max rimane 1.0
        double max = Math.max(1.0,
                Math.max(Math.abs(lf), Math.max(Math.abs(rf),
                        Math.max(Math.abs(lb), Math.abs(rb)))));


        frontLeftMotor.setPower(lf / max);
        frontRightMotor.setPower(rf / max);
        backLeftMotor.setPower(lb / max);
        backRightMotor.setPower(rb / max);
    }

    public void stopDrive() {
        drive(0, 0, 0);
    }
}