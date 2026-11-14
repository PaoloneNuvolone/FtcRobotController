# To Do
[] Calibrare distance_K in Vision

[] Capire se nella mainTeleOp è giusta la telemetro e se effettivamente non cambia sempre pipeline

[] Vedere se l’insegui colore funziona
# Change Log (iniziato il 13/11)
## 13/11 - Venerdì
Aggiunti o modificati i seguenti file:
- BallData
- Vision (Aggiunta principale: getBallData)
- Robot (Aggiunta principale: aggiornare il colore attivo della limelight)
- Drivetrain (Aggiunta principale: aggiunto insegui palla/colore)
- MainTeleOp (Aggiunta principale: implementazione insegui palla e aggiornamento telemetry)

La principale implementazione è il fatto di controllare più pipeline per ottenere colori diversi, 
abbiamo infatti abbandonato l'idea di fare uno script in python perché abbiamo capito che non era 
necessario. 
Adesso abbiamo infatti 3 pipeline diverse (1=red, 2=blue, 3=yellow). 
I file sono ancora in fase di test, infatti domani andiamo a testarli tutti per bene

