package org.firstinspires.ftc.teamcode.opmodes;

import android.annotation.SuppressLint;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.robot.Controls;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.vision.Vision;

@TeleOp(name = "Main TeleOp Prova", group = "TeleOp")
public class MainTeleOp extends LinearOpMode {

    @SuppressLint("DefaultLocale")
    @Override
    public void runOpMode() {

        Robot robot;
        Controls controls = new Controls(gamepad1,gamepad2); // Da i parametri del gamepad1 e gamepad2 alla classe Controls,
        // Ricorda: i gamepad vanno dati all'interno di public void runOpMode perché prima non c'è nessun valore per i controller

        try{    // try{}catch serve come sicurezza per avvertire il driver degli errori nella inizializzazione
            // Richiamo le funzioni di inizializzazione
            robot = new Robot(hardwareMap); // Richiama la funzione Robot in robot.Robot e gli da i parametri hardwareMap
            telemetry.addLine("La vespa è in moto e pronta a partire \nBasta ingranare la prima marcia");
            telemetry.update();

        } catch (Exception e) {

            telemetry.addData("Errore durante l'inizializzazione: ", e.getMessage());
            telemetry.update();

            sleep(5000);
            return; // Termina l'op mode
        }

        waitForStart();
        robot.start(); // avvia anche la limelight, se presente

        while (opModeIsActive()) {
            robot.update();


            /// -GUIDA-

            double strafe  = controls.strafe();
            double forward = -controls.forward();
            double turn    = controls.turn();
            double s       = controls.driveScale();



            /// -VISIONE -

            robot.vision.pipeLineSwitch(1); // 1=red, 2=blue, 3=yellow

            // Un solo comando di guida per ciclo
            robot.drivetrain.drive(strafe*s,forward*s,turn*s);



            /// -TELEMETRY-

            telemetry.addData("Slow mode", controls.isSlowMode() ? "ATTIVA (40%)" : "DISATTIVA (100%)");
            telemetry.addData("Drive Power", "X: %.2f | Y: %.2f | Turn: %.2f",
                    controls.strafe(), controls.forward(), controls.turn());

            telemetry.addData("Limelight", robot.vision.isAvailable());
            telemetry.addData("Tx ",robot.vision.getTx());
            telemetry.addData("Ta",robot.vision.getTa());
            telemetry.addData("distance", robot.vision.getDistance());


            telemetry.addData("Inseguimento palla",controls.isChasingBall());

            telemetry.update();
        }


        robot.stop();
    }
}
