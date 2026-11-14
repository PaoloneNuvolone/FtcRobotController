package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.vision.BallData;

public class Drivetrain {

    // Nomi dei dispositivi nella Robot Configuration
    private static final String FRONT_LEFT_MOTOR = "frontleftmotor";
    private static final String FRONT_RIGHT_MOTOR = "frontrightmotor";
    private static final String BACK_LEFT_MOTOR = "backleftmotor";
    private static final String BACK_RIGHT_MOTOR = "backrightmotor";

    // Motori della trasmissione
    private DcMotor frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor;

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

        // Normalizza: se qualcuno supera 1, scala tutti a 1
        double max = Math.max(1.0,
                Math.max(Math.abs(lf), Math.max(Math.abs(rf),
                Math.max(Math.abs(lb), Math.abs(rb)))));

        frontLeftMotor.setPower(lf / max);
        frontRightMotor.setPower(rf / max);
        backLeftMotor.setPower(lb / max);
        backRightMotor.setPower(rb / max);
    }

    /// *---* Funzione Insegui Palla *---*
    public void driveToBall (BallData ball, double targetCm){
        if (ball == null){
            stopDrive();
            return;
        }

        final double KP_TURN     = 0.02;  // guadagno per la rotazione (da tarare)
        final double KP_FORWARD  = 0.015; // guadagno per l'avanzamento (da tarare)
        final double MAX_TURN    = 0.4;
        final double MAX_FORWARD = 0.5;
        final double DEAD_TURN   = 1.5;   // gradi: sotto non corregge la rotazione
        final double DEAD_DIST   = 3.0;   // cm: sotto non corregge la distanza

        double turn = 0;
        if (Math.abs(ball.tx) > DEAD_TURN) {
            turn = ball.tx * KP_TURN;
            turn = Math.max(-MAX_TURN, Math.min(MAX_TURN, turn));
        }
        double error = ball.distance - targetCm;
        double forward = 0;
        if (Math.abs(error) > DEAD_DIST) {
            forward = error * KP_FORWARD;
            forward = Math.max(-MAX_FORWARD, Math.min(MAX_FORWARD, forward));
        }

        drive(0, forward, turn);
    }


    public void stopDrive() {
        drive(0, 0, 0);
    }

}
