# Odometria a due pod goBILDA — Guida completa

**Progetto:** Market0-ha-sonno — FTC SDK 12.0 (stagione BIOBUZZ 2026‑27)
**Linguaggio:** Java 8
**Ultimo aggiornamento:** 19/11/2026

Questo documento spiega come funziona l'odometria a due pod, come si implementa in Java, come si sceglie e si calibra il modello di pod goBILDA, come far partire il robot da qualunque punto del campo e come ottenere l'angolazione del robot. Alla fine c'è una sezione con le pratiche delle migliori squadre FTC.

> **Nota importante:** l'odometria non è un GPS. Misura **spostamenti relativi**; per sapere dove si trova il robot bisogna dirglielo con `setPose(x, y, heading)` all'inizio di ogni OpMode. Partire da qualunque punto funziona, ma la posa di partenza va impostata (o misurata). Se non la imposti, il robot parte da `(0, 0, 0)` e tutte le coordinate di campo saranno sbagliate.

> **Verifica stagionale BIOBUZZ 2026‑27:** dimensioni del campo, orientamento red/blue e AprilTag sono stati controllati sul Competition Manual 2026‑27 (TU08/TU09) e sul Field Setup Guide 2026‑27. Il field coordinate system FTC è invariato; per BIOBUZZ è il caso "inverted alliance area" (red a sinistra dalla vista del pubblico), quindi +X punta verso il pubblico e +Y verso il lato blu. Il nostro frame top-left resta separato: non mescolarli.

---

## Indice

1. [Cosa vuol dire "odometria a due pod"](#1-cosa-vuol-dire-odometria-a-due-pod)
2. [Quale pod goBILDA abbiamo?](#2-quale-pod-gobilda-abbiamo)
3. [Il sistema di coordinate: top-left = (0,0)](#3-il-sistema-di-coordinate-top-left--00)
4. [Hardware e Robot Configuration](#4-hardware-e-robot-configuration)
5. [La matematica, passo per passo](#5-la-matematica-passo-per-passo)
6. [Il codice `Odometry.java`](#6-il-codice-odometryjava)
7. [Partire da qualunque punto del campo](#7-partire-da-qualunque-punto-del-campo)
8. [L'angolazione: precisione, drift e due vs tre pod](#8-langolazione-precisione-drift-e-due-vs-tre-pod)
9. [Come programmano le migliori squadre](#9-come-programmano-le-migliori-squadre)
10. [Calibrazione e test](#10-calibrazione-e-test)
11. [Field-centric e autonomo](#11-field-centric-e-autonomo)
12. [Errori comuni](#12-errori-comuni)
13. [Checklist finale](#13-checklist-finale)
14. [Riferimenti](#14-riferimenti)

---

## 1. Cosa vuol dire "odometria a due pod"

L'odometria è la tecnica con cui il robot stima la propria posa — posizione `(x, y)` e orientamento `θ` — integrando nel tempo gli spostamenti misurati dai sensori. È un **dead reckoning**: non usa GPS né riferimenti esterni; somma i piccoli spostamenti a ogni ciclo.

Con **due pod** goBILDA si usano due ruote folli (dette *dead wheel*) dotate di encoder:

- **pod forward** (parallelo alla direzione avanti del robot): misura lo spostamento **avanti/indietro**;
- **pod strafe** (perpendicolare): misura lo spostamento **laterale**;
- **IMU** del Control Hub: misura la **rotazione** (heading).

Perché non usare gli encoder dei motori di guida? Perché le ruote mecanum slittano, soprattutto in strafe e in rotazione. I pod sono ruote indipendenti, premute a terra da una molla: misurano il movimento reale del telaio, non quello dei motori.

L'odometria dà:

- `x`, `y`: posizione del **centro di rotazione** del robot nel frame del campo;
- `θ`: orientamento del robot (heading);
- quindi la **posa** `(x, y, θ)`.

Non dà la posizione assoluta: quella dipende dalla posa iniziale che gli fornisci.

---

## 2. Quale pod goBILDA abbiamo?

I pod goBILDA più comuni sono due. Non serve sapere il modello a memoria: misuralo.

| Modello | Ruota | Encoder | Tick/mm | cm/tick | Come riconoscerlo |
|---|---:|---:|---:|---:|---|
| **4-Bar Odometry Pod** (32 mm) | 32 mm | 2000 CPR | 19.894 | 0.0050265 | linkage a 4 barre, ruota piccola, profilo compatto |
| **Swingarm Odometry Pod** (48 mm) | 48 mm | 2000 CPR* | 13.263 | 0.0075398 | braccio oscillante, ruota da 48 mm |

\* I pod goBILDA dichiarano 2000 "countable events" (CPR/PPR) per giro. Se usi un encoder diverso, verifica il suo CPR: il metodo di calcolo è lo stesso. **Non fidarti solo del datasheet: misura con il test di calibrazione** (sezione 10). Fonti: pagina goBILDA del 4-Bar (2000 PPR) e manuale Pinpoint (2000 CPR, 19.894 tick/mm per il 4-Bar; ~13.263 tick/mm per lo Swingarm).

### Come identificare il tuo

1. **Misura il diametro della ruota** con un calibro: 32 mm o 48 mm.
2. Guarda la forma: 4 barre = 4-Bar; braccio con molla = Swingarm.
3. Cerca l'etichetta/SKU sul pod o sulla scatola:
   - 4-Bar 32 mm: `3110-0001-0002`
   - Swingarm 48 mm: `3110-0001-0001` (verifica la tua versione)
4. Se non trovi nulla, fai il test: spingi il robot di 100 cm, leggi i tick, calcola `cm/tick = 100 / tick`. Confronta con la tabella.

### Formula universale

```text
circonferenza = π × diametro_ruota
cmPerTick     = circonferenza / tick_per_giro
ticksPerCm    = tick_per_giro / circonferenza
```

Esempi:

```text
4-Bar 32 mm:  π × 3.2 / 2000 = 0.0050265 cm/tick  (198.94 tick/cm)
Swingarm 48:  π × 4.8 / 2000 = 0.0075398 cm/tick  (132.63 tick/cm)
```

Nel codice userai `CM_PER_TICK`; se il test dice un valore leggermente diverso, **usa quello misurato**: la ruota reale, la compressione e la molla contano più del diametro nominale.

### I pod sono "dead": non si alimentano

I pod hanno un encoder e una ruota folle. Nel Robot Configuration si configurano come **motori** solo per usare la porta encoder, ma **non vanno alimentati**: non chiamare `setPower`. Si legge solo `getCurrentPosition()`.

---

## 3. Il sistema di coordinate: top-left = (0,0)

Questa è la convenzione scelta per il nostro codice, come richiesto: **l'angolo in alto a sinistra del campo è `(0, 0)`**.

### Come guardare il campo

Assunzione: si guarda il campo **dal pubblico/audience**, con il muro di fondo in alto. Nel manuale BIOBUZZ 2026‑27 la red alliance area è a sinistra dalla vista principale del pubblico (sezione ALLIANCE AREA), quindi il lato blu è a destra. In altre stagioni la disposizione può cambiare: la convenzione qui sotto non dipende dall'alleanza, dipende solo dalla tua vista. Se la tua vista è diversa, specchia gli assi e aggiorna la convenzione in tutto il codice (una sola volta, non in punti diversi).

```text
(0,0)  angolo in alto a sinistra
   +X (destra nella vista dal pubblico; in BIOBUZZ il lato blu)
    ------------------------------->
   |                               |
   |        CAMPO 12 x 12 ft       |
   |      365.76 x 365.76 cm       |
   |                               |
   |                               |
   v
   +Y (giù, verso il pubblico)
```

- Origine `(0, 0)`: angolo in alto a sinistra.
- `+X`: verso destra (lungo il muro di fondo; in BIOBUZZ il lato blu).
- `+Y`: verso il basso (verso il pubblico).
- Il campo è un quadrato di **144 in nominali** (365.76 cm di conversione esatta; il manuale BIOBUZZ 2026‑27 riporta ~365.75 cm e una tolleranza di ±1 in / ±2.5 cm). La dimensione imperiale è quella che governa: usa 365.76 cm come nominale, ma il campo reale può variare.
- Gli angoli sono:
  - alto-sinistra `(0, 0)`
  - alto-destra `(365.76, 0)`
  - basso-sinistra `(0, 365.76)`
  - basso-destra `(365.76, 365.76)`

### Heading (angolazione)

Per questa convenzione:

```text
0°   = robot che punta in alto (verso il muro di fondo)
90°  = robot che punta a destra (in BIOBUZZ il lato blu)
180° = robot che punta in basso (verso il pubblico)
270° = robot che punta a sinistra (in BIOBUZZ verso il lato rosso)
```

**Heading positivo = senso orario** (0 → 90 → 180 → 270). È la convenzione "da schermo": comoda perché `+X` è destra e `+Y` è giù.

L'IMU invece restituisce lo yaw **antiorario** (convenzione fisica standard). Nel codice la conversione è:

```text
heading_display = -yaw_imu + offset
```

Per questo nel codice c'è `IMU_YAW_SIGN = -1.0`. Se un giorno cambi convenzione, cambi quel segno e le formule di rotazione, non tutto il resto.

### La posa è il centro di rotazione, non l'angolo del telaio

Il punto `(x, y)` è il **centro di rotazione** del robot (o il punto che scegli come riferimento). Se il robot è contro il muro di sinistra ed è largo 40 cm, il suo centro è a circa 20 cm dal muro: la posa di partenza sarà `x ≈ 20`, non `x = 0`. L'origine è l'angolo del **campo**, non l'angolo del robot.

Esempi di pose di partenza:

| Situazione | Posa `(x, y, heading)` |
|---|---|
| Centro del campo, rivolto verso il muro di fondo | `(182.88, 182.88, 0°)` |
| Vicino all'angolo in alto a sinistra, rivolto verso il basso | `(30, 30, 180°)` |
| Contro il muro di sinistra, rivolto verso destra | `(25, 180, 90°)` |
| Vicino al pubblico, rivolto verso il fondo | `(180, 330, 0°)` |

Questi numeri sono esempi: vanno misurati sulla tua posizione reale, con il metro, dal centro del robot.

### Attenzione ai frame diversi

Esistono almeno quattro frame diversi:

| Frame | Origine | +X | +Y | Heading |
|---|---|---|---|---|
| **FTC ufficiale** | centro del campo | verso destra guardando dal muro rosso | verso il muro opposto (lontano dal muro rosso) | positivo antiorario |
| **Questo documento** | angolo in alto a sinistra (vista dal pubblico) | destra | giù (verso il pubblico) | 0 = su, positivo orario |
| **Pedro Pathing** | angolo in basso a sinistra (secondo la conversione ufficiale) | destra | su | 0 = destra, positivo antiorario |
| **Road Runner** | dipende dalla posa iniziale | avanti | sinistra | positivo antiorario |

La FTC pubblica un *field coordinate system* ufficiale: origine al centro del campo, +X parallelo al muro rosso e crescente verso destra guardando dal muro rosso, +Y perpendicolare e crescente allontanandosi dal muro rosso, +Z verso l'alto; le rotazioni positive sono antiorarie guardando lungo l'asse positivo (documento "FTC Field Coordinate System Definition"). Pedro Pathing usa +X a destra e +Y in alto, heading 0 = destra e positivo antiorario; Road Runner usa +X avanti, +Y sinistra e heading positivo antiorario.

**Caso BIOBUZZ:** la red alliance area è a sinistra dalla vista del pubblico. Applicando la definizione FTC, il muro rosso è la parete sinistra, **+X punta verso il pubblico** (davanti) e **+Y punta verso il lato blu**. È il caso "Square Field (Inverted alliance area)" della pagina FTC. Non è il nostro frame top-left: non usare i due frame insieme.

Questo documento usa un frame **custom** perché la richiesta è "angolo in alto a sinistra = (0,0)". Non mescolare due frame nello stesso codice: scegli questo e converti solo dove serve (es. quando passi i punti a una libreria).

---

## 4. Hardware e Robot Configuration

### Montaggio meccanico

Questa è la parte che le migliori squadre curano di più: l'odometria software non può correggere un pod che non tocca, che flette o che slitta.

- I due pod sono montati **paralleli agli assi del robot**: uno misura l'avanti, l'altro il laterale.
- Devono essere **premuti a terra con una molla** e mantenere il contatto anche sulle piastrelle morbide.
- Il telaio non deve flettere: se il pod si muove rispetto al robot, l'odometria inventa spostamenti.
- La ruota deve essere pulita e non deve strisciare contro il telaio.
- Il cavo encoder va fissato con una fascetta, senza tirare sul connettore.
- Il baricentro basso aiuta; il robot non deve "saltellare" durante le accelerazioni.

### Offset dei pod

Gli offset si misurano dal **centro di rotazione** al punto di contatto del pod, **perpendicolarmente alla direzione in cui il pod rotola**:

- **pod forward**: offset **laterale**; se il pod è a sinistra del centro, l'offset è positivo.
- **pod strafe**: offset **longitudinale**; se il pod è davanti al centro, l'offset è positivo.

La distanza del pod **lungo** la sua direzione di rotolamento non conta: per il pod forward conta solo quanto è spostato lateralmente, per il pod strafe solo quanto è spostato avanti/indietro.

Esempio:

```text
Robot visto dall'alto:

          +X (avanti)
             ^
             |
   pod F     |        pod S
   (a sinistra,       (davanti,
    offset +12 cm)     offset +15 cm)
             |
   ----------+----------> +Y (sinistra)
```

In questo esempio:

```java
FORWARD_POD_LEFT_CM    = +12.0;  // 12 cm a sinistra del centro
STRAFE_POD_FORWARD_CM  = +15.0;  // 15 cm davanti al centro
```

Se il pod forward è a destra, l'offset è negativo. Se il pod strafe è dietro, l'offset è negativo.

### Robot Configuration

Nel Driver Station, nella configurazione del robot:

- aggiungi i due pod come **motori** (serve la porta encoder), con nomi chiari: `forwardPod`, `strafePod`;
- aggiungi l'IMU (di solito si chiama `imu`);
- collega i cavi encoder dei pod alle porte encoder delle porte motore;
- se usi porte motore libere, va bene; se usi le porte dei motori di guida, i nomi saranno quelli dei motori (es. `leftFront`), e dovrai cambiarli nelle costanti.

Suggerimento (da gm0, non da un documento FIRST): nei REV Hub le porte encoder **0 e 3** usano il Quadrature Encoder Interface (QEI) e sono più affidabili ad alta velocità; le porte 1 e 2 usano timer generici e possono perdere conteggi con encoder ad alto CPR come i pod. Se puoi, metti i pod sulle porte 0 e 3. Non è obbligatorio, ma è una pratica comune.

### IMU

L'IMU va inizializzato con l'orientamento reale del Control Hub:

```java
RevHubOrientationOnRobot orientation = new RevHubOrientationOnRobot(
        RevHubOrientationOnRobot.LogoFacingDirection.UP,
        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
imu.initialize(new IMU.Parameters(orientation));
imu.resetYaw();
```

Le direzioni `UP`/`FORWARD` devono corrispondere a come è montato il Control Hub sul robot. Nel repo c'è il sample `ConceptExploringIMUOrientation`: usalo per trovare la combinazione giusta. Se l'orientamento è sbagliato, l'heading ruota nel verso sbagliato o non corrisponde ai movimenti.

### Bulk caching

Il progetto usa già `BulkCachingMode.MANUAL`. Nell'`update()` del robot:

```java
for (LynxModule hub : hubs) hub.clearBulkCache();
odometry.update();
```

La lettura degli encoder dei pod e dell'IMU deve avvenire **dopo** `clearBulkCache()` e **prima** di calcolare i comandi di guida.

---

## 5. La matematica, passo per passo

### Variabili

- `dF`: spostamento del pod forward in cm (positivo avanti).
- `dS`: spostamento del pod strafe in cm (positivo a sinistra).
- `dTheta`: rotazione del robot in radianti (positiva oraria, nella nostra convenzione).
- `dx`: spostamento del centro nella direzione avanti del robot.
- `dy`: spostamento del centro nella direzione sinistra del robot.

### 1. Tick → centimetri

```text
dF = (tickF - tickF_prec) × CM_PER_TICK × SIGN_FORWARD
dS = (tickS - tickS_prec) × CM_PER_TICK × SIGN_STRAFE
```

`SIGN_FORWARD` e `SIGN_STRAFE` servono a far crescere i valori nella direzione attesa. Si verificano con i test della sezione 10.

### 2. Correzione della rotazione

I pod non sono nel centro di rotazione. Quando il robot ruota, il pod forward a sinistra viene trascinato **avanti**, e il pod strafe davanti viene trascinato **a destra** (cioè negativo a sinistra). Le letture contengono quindi una parte di rotazione che non è traslazione.

Con heading positivo **orario** (la nostra convenzione), le formule sono:

```text
dx = dF - offsetY_forward × dTheta
dy = dS + offsetX_strafe  × dTheta
```

dove:

- `offsetY_forward` è l'offset laterale del pod forward (sinistra positiva);
- `offsetX_strafe` è l'offset longitudinale del pod strafe (avanti positivo).

Verifica con un caso puro: robot che ruota in senso orario, `dTheta > 0`, nessuna traslazione.

- Pod forward a sinistra (`offsetY > 0`): viene trascinato avanti, quindi `dF > 0`. La formula dà `dx = dF - offsetY × dTheta = 0`. Corretto.
- Pod strafe davanti (`offsetX > 0`): viene trascinato a destra, quindi `dS < 0` (perché `dS` è positivo a sinistra). La formula dà `dy = dS + offsetX × dTheta = 0`. Corretto.

Se gli offset hanno il segno sbagliato, questa correzione **aggiunge** errore invece di toglierlo: è il motivo per cui il test di rotazione a 360° è il più importante.

### 3. Da frame robot a frame campo

Nel frame del robot:

- `dx` = avanti
- `dy` = sinistra

Nel frame del campo:

- `+X` = destra
- `+Y` = giù
- heading `θ` = 0 in alto, positivo orario

La rotazione è:

```text
dX =  dx × sin(θ) - dy × cos(θ)
dY = -dx × cos(θ) - dy × sin(θ)
```

Verifica con `θ = 0` (robot rivolto in alto):

- avanti `dx > 0` → `dX = 0`, `dY = -dx` (si va in alto, quindi `Y` diminuisce);
- sinistra `dy > 0` → `dX = -dy` (si va a sinistra, quindi `X` diminuisce), `dY = 0`.

Verifica con `θ = 90°` (robot rivolto a destra):

- avanti `dx > 0` → `dX = dx`, `dY = 0` (si va a destra);
- sinistra `dy > 0` → `dX = 0`, `dY = -dy` (la sinistra del robot è in alto, quindi `Y` diminuisce).

### 4. Integrazione

```text
mid = θ_prec + dTheta / 2

x = x + dX(mid)
y = y + dY(mid)
θ = θ_prec + dTheta
```

Si usa `mid` (heading a metà ciclo) perché la rotazione avviene durante il ciclo: usare l'heading iniziale o finale introduce un piccolo errore. A 50–100 Hz l'errore è trascurabile; a rotazioni molto veloci o loop lenti si può usare l'integrazione esatta ("pose exponential"), che è quella usata da Road Runner e Pedro. Per il nostro codice, `mid` è più che sufficiente.

### 5. Normalizzazione dell'angolo

L'angolo va normalizzato tra `-π` e `+π`:

```java
dTheta = AngleUnit.normalizeRadians(theta - headingRad);
```

Senza normalizzazione, quando l'IMU passa da `+π` a `-π` il robot "salta" di 360° e la posa impazzisce.

### Esempio numerico

Posa iniziale `(100, 100, 0°)`, robot rivolto in alto.

| Movimento | Posa dopo |
|---|---|
| avanti 10 cm | `(100, 90)` |
| a sinistra 10 cm | `(90, 100)` |
| a destra 10 cm | `(110, 100)` |
| indietro 10 cm | `(100, 110)` |

Con heading `90°` (rivolto a destra):

| Movimento | Posa dopo |
|---|---|
| avanti 10 cm | `(110, 100)` |
| a sinistra 10 cm | `(100, 90)` (la sinistra è in alto) |
| indietro 10 cm | `(90, 100)` |

Con heading `180°` (rivolto in basso):

| Movimento | Posa dopo |
|---|---|
| avanti 10 cm | `(100, 110)` |
| a sinistra 10 cm | `(110, 100)` (la sinistra è a destra) |

Questi esempi sono il modo più veloce per capire se il segno dell'heading e le formule sono giusti.

---

## 6. Il codice `Odometry.java`

Questo è un template completo per il file:

```text
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/odometry/Odometry.java
```

```java
package org.firstinspires.ftc.teamcode.odometry;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

/**
 * Odometria a due pod goBILDA + IMU.
 *
 * Frame del campo:
 *   (0,0) = angolo in alto a sinistra (vista dal pubblico)
 *   +X = destra (verso il lato blu)
 *   +Y = giu' (verso il pubblico)
 *   heading = 0 in alto, 90 destra, 180 giu', 270 sinistra
 *   heading positivo = orario
 */
public class Odometry {

    // ============================ CONFIGURAZIONE ============================

    // Nomi nella Robot Configuration. Devono essere le porte motore
    // dove sono attaccati gli encoder dei pod.
    private static final String FORWARD_POD_NAME = "forwardPod";
    private static final String STRAFE_POD_NAME  = "strafePod";
    private static final String IMU_NAME         = "imu";

    // 4-Bar 32 mm: 3.2 cm, 2000 CPR -> 0.0050265 cm/tick
    // Swingarm 48 mm: 4.8 cm, 2000 CPR -> 0.0075398 cm/tick
    // Se il test di calibrazione dice un valore diverso, usa quello.
    private static final double WHEEL_DIAMETER_CM = 3.2;
    private static final double TICKS_PER_REV     = 2000.0;
    private static final double CM_PER_TICK =
            Math.PI * WHEEL_DIAMETER_CM / TICKS_PER_REV;

    // Offset dal centro di rotazione, in cm.
    // Forward: offset laterale, sinistra positiva.
    // Strafe:  offset longitudinale, avanti positivo.
    private static final double FORWARD_POD_LEFT_CM   = 0.0;
    private static final double STRAFE_POD_FORWARD_CM = 0.0;

    // Segni: +1 se il conteggio cresce nella direzione attesa.
    // Forward: +1 se spingendo avanti i tick crescono.
    // Strafe:  +1 se spingendo a sinistra i tick crescono.
    private static final double SIGN_FORWARD = +1.0;
    private static final double SIGN_STRAFE  = +1.0;

    // L'IMU da' yaw antiorario; la nostra heading e' oraria.
    private static final double IMU_YAW_SIGN = -1.0;

    // ================================ STATO ================================

    private final DcMotorEx forwardPod;
    private final DcMotorEx strafePod;
    private final IMU imu;

    private long lastForwardTicks;
    private long lastStrafeTicks;

    private double xCm;           // destra
    private double yCm;           // giu'
    private double headingRad;    // oraria, 0 = su
    private double headingOffsetRad;
    private boolean available;

    // ============================== COSTRUTTORE =============================

    public Odometry(HardwareMap hw) {
        forwardPod = hw.tryGet(DcMotorEx.class, FORWARD_POD_NAME);
        strafePod  = hw.tryGet(DcMotorEx.class, STRAFE_POD_NAME);
        imu        = hw.tryGet(IMU.class, IMU_NAME);

        if (imu != null) {
            // Questa orientazione deve corrispondere a come e' montato il Control Hub.
            RevHubOrientationOnRobot orientation = new RevHubOrientationOnRobot(
                    RevHubOrientationOnRobot.LogoFacingDirection.UP,
                    RevHubOrientationOnRobot.UsbFacingDirection.FORWARD);
            imu.initialize(new IMU.Parameters(orientation));
        }

        available = forwardPod != null && strafePod != null && imu != null;
        reset();
    }

    /** Chiamare all'inizio dell'OpMode, con il robot fermo. */
    public void start() {
        reset();
    }

    /** Azzera posa e heading: il robot e' nell'origine, rivolto in alto. */
    public void reset() {
        if (forwardPod != null) lastForwardTicks = forwardPod.getCurrentPosition();
        if (strafePod != null)  lastStrafeTicks  = strafePod.getCurrentPosition();
        if (imu != null) imu.resetYaw();

        xCm = 0;
        yCm = 0;
        headingRad = 0;
        headingOffsetRad = 0;
    }

    /**
     * Imposta la posa di partenza nel frame del campo.
     *
     * @param xCm        cm verso destra dall'angolo in alto a sinistra
     * @param yCm        cm verso il basso dall'angolo in alto a sinistra
     * @param headingDeg 0 = su, 90 = destra, 180 = giu', 270 = sinistra
     */
    public void setPose(double xCm, double yCm, double headingDeg) {
        this.xCm = xCm;
        this.yCm = yCm;
        this.headingRad = AngleUnit.normalizeRadians(Math.toRadians(headingDeg));

        if (imu != null) {
            this.headingOffsetRad = this.headingRad - IMU_YAW_SIGN * imuYaw();
        } else {
            this.headingOffsetRad = this.headingRad;
        }

        // Riallinea i contatori: gli spostamenti precedenti non contano.
        if (forwardPod != null) lastForwardTicks = forwardPod.getCurrentPosition();
        if (strafePod != null)  lastStrafeTicks  = strafePod.getCurrentPosition();
    }

    /** Da chiamare una volta per ciclo, dopo clearBulkCache(). */
    public void update() {
        if (!available) return;

        long f = forwardPod.getCurrentPosition();
        long s = strafePod.getCurrentPosition();

        double dF = (f - lastForwardTicks) * CM_PER_TICK * SIGN_FORWARD;
        double dS = (s - lastStrafeTicks)  * CM_PER_TICK * SIGN_STRAFE;
        lastForwardTicks = f;
        lastStrafeTicks  = s;

        double theta = IMU_YAW_SIGN * imuYaw() + headingOffsetRad;
        double dTheta = AngleUnit.normalizeRadians(theta - headingRad);
        double mid = headingRad + dTheta / 2.0;

        // Da frame robot (x avanti, y sinistra) a spostamenti del centro.
        double dx = dF - FORWARD_POD_LEFT_CM * dTheta;
        double dy = dS + STRAFE_POD_FORWARD_CM * dTheta;

        // Da frame robot a frame campo (X destra, Y giu').
        xCm += dx * Math.sin(mid) - dy * Math.cos(mid);
        yCm += -dx * Math.cos(mid) - dy * Math.sin(mid);

        headingRad = AngleUnit.normalizeRadians(headingRad + dTheta);
    }

    private double imuYaw() {
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
    }

    // ================================ GETTER ================================

    public boolean isAvailable()  { return available; }
    public double getX()          { return xCm; }
    public double getY()          { return yCm; }
    public double getHeadingRad() { return headingRad; }
    public double getHeadingDeg() { return Math.toDegrees(headingRad); }

    /** Pose2D con la NOSTRA heading di display, non il frame FTC ufficiale. */
    public Pose2D getPose() {
        return new Pose2D(DistanceUnit.CM, xCm, yCm,
                AngleUnit.DEGREES, Math.toDegrees(headingRad));
    }

    /** Tick grezzi: utili per la calibrazione. */
    public int getForwardTicks() {
        return forwardPod != null ? forwardPod.getCurrentPosition() : 0;
    }

    public int getStrafeTicks() {
        return strafePod != null ? strafePod.getCurrentPosition() : 0;
    }
}
```

### Integrazione in `Robot`

Nel tuo `Robot`:

```java
public final Drivetrain drivetrain;
public final Vision vision;
public final Odometry odometry;

public Robot(HardwareMap hw) {
    // ... bulk caching ...

    drivetrain = new Drivetrain(hw);
    odometry   = new Odometry(hw);
    vision     = new Vision(hw);
}

public void start() {
    odometry.start();
    vision.start(); // rendi Vision.start() null-safe se puoi staccare la Limelight
}

public void update() {
    for (LynxModule hub : hubs) hub.clearBulkCache();
    odometry.update();
    vision.update();
}
```

L'ordine è importante: `clearBulkCache()` → `odometry.update()` → calcolo dei comandi → `drivetrain.drive(...)`.

### Field-centric

Nel `Drivetrain`:

```java
/**
 * fieldForward: positivo verso l'alto del campo
 * fieldRight:   positivo verso destra del campo
 * headingRad:   heading del robot (0 = su, positivo orario)
 */
public void driveFieldRelative(double fieldForward, double fieldRight,
                               double turn, double headingRad) {
    double c = Math.cos(headingRad);
    double s = Math.sin(headingRad);

    double forwardBody = fieldForward * c + fieldRight * s;
    double strafeBody  = -fieldForward * s + fieldRight * c;

    drive(strafeBody, forwardBody, turn);
}
```

Nel `MainTeleOp`:

```java
// ATTENZIONE: qui vogliamo positivo = avanti.
// Se il tuo MainTeleOp usa gia' -controls.forward(), allinea i segni:
// il field-centric non corregge un segno sbagliato, lo ruota soltanto.
double forward = controls.forward();
double strafe  = controls.strafe();
double turn    = controls.turn(); // se il robot gira al contrario, usa -controls.turn()

if (fieldCentric && robot.odometry.isAvailable()) {
    robot.drivetrain.driveFieldRelative(forward, strafe, turn,
            robot.odometry.getHeadingRad());
} else {
    robot.drivetrain.drive(strafe, forward, turn);
}
```

**Attenzione ai segni:** nel tuo `MainTeleOp` c'è `forward = -controls.forward()`, mentre `Controls.forward()` restituisce già `-left_stick_y`. Sembra una doppia negazione. Anche `Controls.turn()` restituisce `-right_stick_x`, ma nel `Drivetrain` un `turn` positivo gira in senso orario: con lo stick a destra il robot potrebbe girare a sinistra. Prima di usare il field-centric, prova la guida manuale a ruote sollevate e allinea i segni:

- stick avanti → robot avanti;
- stick a destra → robot a destra (strafe);
- stick destro a destra → robot orario.

Il field-centric non corregge i segni, li ruota soltanto. Se il manuale è invertito, correggi `MainTeleOp`/`Controls` (o introduci un `TURN_SIGN` unico) e poi usa lo stesso segno nel field-centric.

### Auto: `driveTo`

```java
private static final double KP_XY      = 0.02;
private static final double KP_HEADING = 0.8;
private static final double MAX_POWER  = 0.6;
private static final double TOL_CM     = 2.0;
private static final double TOL_DEG    = 2.0;
private static final double TURN_SIGN  = +1.0; // -1 se il robot gira al contrario

private boolean driveTo(double targetX, double targetY,
                        double targetHeadingDeg, long timeoutMs) {

    ElapsedTime timer = new ElapsedTime();

    while (opModeIsActive() && timer.milliseconds() < timeoutMs) {
        robot.update();

        double x = robot.odometry.getX();
        double y = robot.odometry.getY();
        double h = robot.odometry.getHeadingRad();

        double ex = targetX - x;
        double ey = targetY - y;

        // errore nel frame del robot
        double forward = ex * Math.sin(h) - ey * Math.cos(h);
        double right   = ex * Math.cos(h) + ey * Math.sin(h);

        double eH = AngleUnit.normalizeRadians(
                Math.toRadians(targetHeadingDeg) - h);

        double turn = TURN_SIGN * KP_HEADING * eH;

        robot.drivetrain.drive(
                clamp(right * KP_XY, MAX_POWER),
                clamp(forward * KP_XY, MAX_POWER),
                clamp(turn, MAX_POWER));

        if (Math.hypot(ex, ey) < TOL_CM
                && Math.abs(Math.toDegrees(eH)) < TOL_DEG) {
            break;
        }
    }

    robot.drivetrain.stopDrive();
    return true;
}

private double clamp(double v, double max) {
    return Math.max(-max, Math.min(max, v));
}
```

Nell'autonomo:

```java
waitForStart();
robot.start();

robot.odometry.setPose(START_X_CM, START_Y_CM, START_HEADING_DEG);

driveTo(100, 100, 90, 3000);
```

---

## 7. Partire da qualunque punto del campo

La richiesta "il robot deve poter partire da qualunque punto" si realizza così:

1. **Misuri** la posizione del centro del robot rispetto all'angolo in alto a sinistra.
2. **Misuri** l'heading iniziale (0 su, 90 destra, 180 giù, 270 sinistra, oppure un angolo intermedio).
3. **Chiami** `setPose(x, y, heading)` all'inizio dell'OpMode, prima di muovere il robot.

Il codice non cambia: cambiano solo i tre numeri. L'odometria è relativa; il frame è un riferimento arbitrario. Funziona allo stesso modo se il robot parte al centro, in un angolo o a metà parete.

### TeleOp vs Auto

- **TeleOp:** se ti serve solo il field-centric, basta l'heading. Se il robot parte sempre rivolto nello stesso verso, `reset()` e heading 0 vanno bene. Se può partire in un verso diverso, chiama `setPose` con l'heading misurato (e con `x, y` misurati se vuoi anche le coordinate assolute). Il field-centric usa l'heading, non `x/y`.
- **Auto:** chiama sempre `setPose` con la posa completa. La routine usa coordinate di campo; se la posa iniziale è sbagliata, tutti i punti sono traslati/ruotati.

### Se non chiami `setPose`

Il robot parte da `(0, 0, 0)`: l'angolo in alto a sinistra, rivolto in alto. Se il robot in realtà è al centro del campo, tutte le coordinate sono sbagliate di 182.88 cm. **L'odometria non può indovinare dove sei**: non è un GPS.

### Esempi

```java
// Angolo in alto a sinistra, centro a 30 cm dai muri, rivolto verso il basso
robot.odometry.setPose(30, 30, 180);

// Centro del campo, rivolto verso il muro di fondo
robot.odometry.setPose(182.88, 182.88, 0);

// Contro il muro di sinistra, rivolto verso destra
robot.odometry.setPose(25, 180, 90);
```

Se la posizione di partenza cambia da match a match, mettila in una costante o in una classe `Constants` e cambia solo quella. Non duplicare i numeri dentro le OpMode.

### Un dettaglio importante

La posa è il **centro di rotazione**. Se il robot è contro il muro, il centro è dentro il campo di almeno metà larghezza. Non usare `x = 0` se il robot è contro il muro: il telaio tocca il muro, non il centro.

---

## 8. L'angolazione: precisione, drift e due vs tre pod

### Da dove viene l'angolo

Con **due pod**, l'heading viene **solo dall'IMU**. I pod misurano traslazione, non rotazione: due ruote non bastano per ricavare l'angolo. Quindi:

- `getHeadingDeg()` è l'angolo dell'IMU, convertito nella convenzione del campo;
- la posizione `x, y` viene ruotata usando quell'angolo: se l'angolo è sbagliato, anche la posizione deriva.

### È possibile avere l'angolo preciso?

Sì, l'IMU dà un angolo con risoluzione molto fine (frazioni di grado), ma è un angolo **relativo** e **deriva** nel tempo. La documentazione FTC conferma che lo yaw aumenta ruotando in senso antiorario e che è relativo all'ultima chiamata di `resetYaw()`. La precisione a breve termine è ottima; l'accuratezza assoluta dipende da:

- quanto è fermo il robot quando fai `resetYaw()`;
- l'orientamento corretto del Control Hub;
- vibrazioni e urti;
- la durata: in un auto da 30 s il drift è piccolo; in una TeleOp da 2:30 può diventare qualche grado.

Un errore di heading si amplifica con la distanza:

```text
1° di errore su 2 m  -> circa 3.5 cm di errore laterale
5° di errore su 2 m  -> circa 17 cm di errore laterale
```

Per questo le migliori squadre non trascurano l'heading: è la parte più delicata dell'odometria a due pod.

### Come migliorare l'angolo

1. **Tre pod** (due paralleli + uno perpendicolare): i due paralleli permettono di calcolare l'heading dalle ruote, senza dipendere solo dall'IMU. È più accurato, ma serve un terzo pod e una `trackWidth` calibrata.
2. **Pinpoint goBILDA**: un coprocessore che legge due pod, ha un IMU e fa la fusione; nel repo c'è il sample `SensorGoBildaPinpoint.java`. È l'upgrade più semplice se non vuoi aggiungere un terzo pod.
3. **IMU migliore/calibrata**: l'IMU del Control Hub va bene, ma va inizializzata con l'orientamento giusto e resettata a robot fermo.
4. **Correzioni esterne**: AprilTag o visione. Il manuale BIOBUZZ 2026‑27 (sezione HIVE Structure) descrive i cluster AprilTag come ausili per navigazione e targeting, ma li mette sulla faccia inferiore delle CELL della HIVE, che è su un pivot e **si ribalta**: non sono landmark fissi. Usali per mirare, non come verità assoluta di posizione. Per l'heading assoluto servirebbe un riferimento fisso (muri, tag statici) che nel campo di gara non è garantito.

### Due pod: verdetto

Per un auto FTC da 30 s e una TeleOp normale, **due pod + IMU sono sufficienti** se:

- i pod sono montati bene;
- gli offset sono corretti;
- l'IMU è calibrata e resettata;
- accetti un piccolo drift nel tempo.

Se vuoi la precisione "da finale mondiale", passa a tre pod o Pinpoint. Ma prima sistema meccanica e calibrazione: un terzo pod con offset sbagliati non salva nessuno.

---

## 9. Come programmano le migliori squadre

Le squadre forti non hanno "un trucco segreto": hanno un **sistema** ripetibile. Queste sono le pratiche che fanno la differenza.

### 9.1 Meccanica prima del codice

- Pod montati rigidi, paralleli agli assi, con molla e contatto costante.
- Ruote pulite, stesso diametro, nessun attrito con il telaio.
- Cavi fissati; nessuna forza sul connettore encoder.
- Il telaio non flette: se flette, l'odometria inventa movimenti.

### 9.2 Una sola fonte di verità per le costanti

- Tutte le costanti in un file `Constants` o nella classe `Odometry`: nomi dei pod, diametro, tick/giro, offset, segni, dimensioni campo.
- Niente numeri magici sparsi nelle OpMode.
- Se possibile, usa `@Config` di FTC Dashboard per vedere/toccare i valori durante il tuning.
- Il progetto ha già la dipendenza `com.acmerobotics.dashboard:dashboard:0.6.0`.

### 9.3 Misura, non indovinare

- Diametro ruota con il calibro.
- Offset con il metro/piombo dal centro di rotazione.
- `CM_PER_TICK` con il test di 100 cm.
- Heading con una rotazione di 90° su una giunzione di piastrelle.
- Mai fidarsi solo del CAD o del datasheet.

### 9.4 IMU fatta bene

- Orientamento `RevHubOrientationOnRobot` corretto.
- `resetYaw()` a robot fermo, all'inizio.
- La documentazione FTC dice che lo yaw dell'IMU aumenta ruotando in senso **antiorario**; il nostro `IMU_YAW_SIGN = -1` lo trasforma in heading oraria. Controlla che ruotando in senso orario l'heading aumenti (nella nostra convenzione).
- Se l'heading non cambia o va nel verso sbagliato, fermati e sistema l'IMU prima di tutto il resto.

### 9.5 Loop veloce e regolare

- `clearBulkCache()` una volta per ciclo.
- `odometry.update()` una volta per ciclo, prima dei comandi.
- Niente `sleep()` nel loop di TeleOp/Auto.
- Usa `ElapsedTime` o `System.nanoTime()` per il `dt`, se ti serve la velocità.
- A 50–100 Hz il `mid-heading` è più che sufficiente.

### 9.6 Matematica pulita

- Normalizza gli angoli (`AngleUnit.normalizeRadians`).
- Usa `double`, non `float`.
- Usa gli offset corretti; controlla il segno con il test di rotazione.
- Se ruoti molto veloce o il loop è lento, valuta l'integrazione esatta (pose exponential), come fanno Road Runner e Pedro.

### 9.7 Posa iniziale sempre

- Ogni OpMode parte con `setPose(...)`.
- Le pose di partenza sono in `Constants`, una per ogni posizione di gara.
- Segna a terra il punto di partenza con nastro/righello: la posa deve essere ripetibile.
- Se la posa iniziale è sbagliata, l'auto è sbagliato anche se la matematica è perfetta.

### 9.8 Test ripetibili

- 100 cm avanti, 100 indietro.
- 100 cm a sinistra, 100 a destra.
- 360° sul posto.
- Quadrato 100 × 100.
- Figure-eight (se vuoi stressare la rotazione).
- Ripeti 3 volte e confronta; se i risultati cambiano, c'è una causa meccanica.

### 9.9 Telemetria e log

- Mostra `x`, `y`, `heading`, tick dei pod.
- Usa FTC Dashboard per grafici e tuning.
- Registra i test; annota cosa hai cambiato.
- Un log vale più di dieci supposizioni.

### 9.10 Fallback e robustezza

- Usa `tryGet` per pod e IMU: se un dispositivo manca, l'OpMode non deve crashare.
- Se l'odometria non è disponibile, la TeleOp resta robot-centrica.
- Se un pod smette di contare, il robot deve almeno fermarsi o degradare in modo controllato.
- Non nascondere gli errori: mostra "odometry unavailable" in telemetria.

### 9.11 Quando serve, upgrade

- Due pod + IMU: buono per la maggior parte dei match.
- Tre pod: heading dalle ruote, più preciso.
- Pinpoint: fusione hardware, meno codice.
- AprilTag: solo riferimenti statici; in BIOBUZZ i tag si muovono, quindi non per la posizione assoluta.

### 9.12 Semplicità e version control

- Una classe `Odometry`, una `Robot`, OpMode sottili.
- Commenti in italiano, nomi coerenti.
- Commit dopo ogni calibrazione che funziona.
- Un compagno di squadra deve poter capire il codice senza di te.

---

## 10. Calibrazione e test

### 10.1 Identifica il pod

| Cosa | Come | Valore atteso |
|---|---|---|
| Diametro ruota | calibro | 32 mm o 48 mm |
| Tick/giro | etichetta o test | 2000 (verifica il tuo encoder) |
| `CM_PER_TICK` | `π × D / tick` | 0.0050265 o 0.0075398 |
| Offset forward | metro dal centro | cm, sinistra + |
| Offset strafe | metro dal centro | cm, avanti + |

### 10.2 Verifica gli encoder

- Spingi il robot avanti: `getForwardTicks()` deve cambiare.
- Spingi a sinistra: `getStrafeTicks()` deve cambiare.
- Se non cambiano: configurazione, porta, cavo, direzione.

### 10.3 Test avanti/indietro (heading 0)

Posa di partenza: `(100, 100, 0)` (rivolto in alto).

| Test | Posa attesa | Se sbagliato |
|---|---|---|
| avanti 100 cm | `(100, 0)` | `SIGN_FORWARD`, `CM_PER_TICK`, offset |
| indietro 100 cm | `(100, 200)` | `SIGN_FORWARD`, `CM_PER_TICK` |
| sinistra 100 cm | `(0, 100)` | `SIGN_STRAFE`, offset |
| destra 100 cm | `(200, 100)` | `SIGN_STRAFE`, offset |

Se spingendo avanti `x` cambia, c'è un problema di heading o di offset: il robot non è davvero a heading 0.

### 10.4 Test di rotazione (il più importante)

Posa di partenza: `(100, 100, 0)`. Fai ruotare il robot di **360° sul posto**, senza traslare.

- Alla fine deve essere di nuovo `(100, 100, 0)`, con heading 0 (o 360°).
- Se `x` deriva lungo l'avanti/indietro: `FORWARD_POD_LEFT_CM` è sbagliato (segno o valore).
- Se `y` deriva lungo il laterale: `STRAFE_POD_FORWARD_CM` è sbagliato.
- Se deriva in cerchio: entrambi gli offset o il segno dell'heading.

Questo test è il modo più diretto per trovare gli offset. Fallo prima di qualsiasi auto.

### 10.5 Test heading

- Ruota il robot di 90° in senso orario (verso destra). `getHeadingDeg()` deve passare da 0 a 90.
- Se va a 270 o -90, il segno è invertito: `IMU_YAW_SIGN` o orientamento del Control Hub.
- Se non cambia, l'IMU non è inizializzata o il nome `imu` è sbagliato.

### 10.6 Test quadrato

Posa di partenza: `(100, 100, 0)`.

1. avanti 100 → `(100, 0)`
2. destra 100 → `(200, 0)`
3. indietro 100 → `(200, 100)`
4. sinistra 100 → `(100, 100)`

Se non torna al punto di partenza, controlla scala, offset e slittamento. Ripeti 3 volte: se l'errore cambia, è meccanico; se è sempre lo stesso, è di calibrazione.

### 10.7 Test di scalatura

- Spingi 100 cm; leggi la distanza.
- Se leggi 95 cm: `CM_PER_TICK = CM_PER_TICK × 100 / 95`.
- Ripeti finché l'errore è sotto l'1–2%.

### 10.8 Test di repeatability

- Fai lo stesso percorso 3 volte dalla stessa posa.
- Se la posa finale cambia di molto, c'è slittamento o un pod che non tocca.
- Se cambia poco, sei pronto per l'auto.

### 10.9 Test sul campo

- Le piastrelle morbide e le giunzioni possono far slittare i pod.
- Prova a batteria carica e scarica: la velocità cambia, l'odometria no, ma lo slittamento sì.
- Prova nella posizione di gara reale, non solo in laboratorio.

---

## 11. Field-centric e autonomo

### 11.1 Field-centric

Con la convenzione del campo:

- "avanti" del driver = verso l'alto del campo (`-Y`);
- "destra" del driver = verso destra del campo (`+X`).

La funzione `driveFieldRelative` della sezione 6 ruota il comando del driver con l'heading del robot. Se il robot parte in un verso diverso, l'heading deve essere corretto: chiama `setPose` con l'heading misurato, oppure ruota il robot nella posizione standard prima di iniziare.

Se il driver preferisce "avanti = nella direzione in cui è rivolto il robot", usa la guida robot-centrica normale (`drive`) e lascia il field-centric come opzione.

### 11.2 Autonomo

Schema:

```java
@Autonomous(name = "Auto Test Odometria", group = "Auto")
public class Auto extends LinearOpMode {
    private Robot robot;

    @Override
    public void runOpMode() {
        robot = new Robot(hardwareMap);
        waitForStart();
        robot.start();

        // Qualunque punto del campo: cambia solo questa riga.
        robot.odometry.setPose(START_X_CM, START_Y_CM, START_HEADING_DEG);

        driveTo(100, 100, 90, 3000);
        driveTo(150, 100, 90, 3000);

        robot.stop();
    }

    // driveTo() e clamp() della sezione 6
}
```

Con `setPose` puoi partire da qualsiasi punto: la routine usa coordinate assolute del campo, ma l'odometria sa dove sei perché gliel'hai detto.

### 11.3 Punti di campo

- Tieni una mappa dei punti in `Constants`: `START_RED_LEFT`, `START_BLUE_RIGHT`, `LAUNCH_RED`, ecc.
- Ogni punto è `(x, y, heading)` nel frame top-left.
- Se cambi la posizione di partenza, cambi una costante, non la logica.
- Verifica i punti sul campo con il metro: un `driveTo` preciso su coordinate sbagliate porta nel posto sbagliato.

### 11.4 Se un giorno passi a Pedro o Road Runner

Le due librerie hanno i loro localizer (two-wheel, Pinpoint, OTOS, three-wheel) e il loro frame. Pedro usa +X a destra, +Y in alto, heading 0 = destra e positivo antiorario; Road Runner usa +X avanti, +Y sinistra e heading positivo antiorario. Pedro dichiara che tutti i localizer tranne OTOS usano la pose exponential; Road Runner usa la stessa idea di integrazione. Non mescolare il nostro `Odometry` con il loro `Follower` nella stessa OpMode. Puoi:

- usare `Odometry` per la TeleOp e Pedro/Road Runner per l'auto;
- oppure migrare tutto al loro localizer e usare il loro `Follower`.

In entrambi i casi, la matematica di questo documento ti serve per capire cosa succede e per tarare i loro offset.

---

## 12. Errori comuni

| Sintomo | Causa probabile | Correzione |
|---|---|---|
| Dopo un giro di 360° la posa è spostata | offset dei pod sbagliati (segno o valore) | rifai il test di rotazione; misura gli offset dal centro |
| Spingendo avanti `x` cambia | heading non 0, offset sbagliato, formula | metti il robot a heading 0; controlla `SIGN_FORWARD` e offset |
| Spingendo a sinistra `y` cambia | `SIGN_STRAFE` o offset | controlla il segno; rifai il test |
| L'heading diminuisce ruotando in senso orario | `IMU_YAW_SIGN` o orientamento del Control Hub | cambia segno/orientamento |
| Il field-centric va di lato | heading sbagliato, "avanti" del driver diverso, turn invertito | controlla `setPose`, la direzione di "avanti", `TURN_SIGN` |
| Lo stick avanti fa andare indietro il robot | doppia negazione: `Controls.forward()` e `MainTeleOp` negano entrambi | togli un meno e prova a ruote sollevate |
| Lo stick destro gira dal lato sbagliato | `Controls.turn()` nega lo stick, ma nel drivetrain `turn` positivo = orario | togli il meno o usa un `TURN_SIGN` unico |
| `setPose` fa saltare la posa | offset di heading o contatori non riallineati | il codice riallinea i contatori; controlla l'IMU |
| I pod non contano | configurazione, porta, cavo, encoder | controlla `getCurrentPosition()` spingendo a mano |
| L'odometria deriva nel tempo | drift dell'IMU | tre pod o Pinpoint; reset a fermo |
| L'auto è spostato di una costante | posa di partenza sbagliata | misura di nuovo il centro del robot |
| Crash se manca un dispositivo | `get` invece di `tryGet` | usa `tryGet` e `isAvailable` |
| La TeleOp non parte senza Limelight | `Vision.start()` non fa null-check | aggiungi il controllo (non c'entra con l'odometria) |

---

## 13. Checklist finale

### Prima di ogni match

- [ ] Pod puliti e a contatto con le piastrelle.
- [ ] Molla carica, cavi fissati.
- [ ] IMU calibrata e `resetYaw()` a robot fermo.
- [ ] Posa di partenza misurata e inserita in `setPose`.
- [ ] Heading iniziale corretto (0 su, 90 destra, 180 giù, 270 sinistra).
- [ ] Telemetria `x, y, heading` visibile.
- [ ] Batteria carica.

### Dopo ogni modifica meccanica

- [ ] Rifai il test avanti/indietro.
- [ ] Rifai il test di rotazione a 360°.
- [ ] Rifai il test quadrato.
- [ ] Aggiorna gli offset se hai spostato i pod.

### Prima di un auto nuovo

- [ ] `setPose` con la posa reale.
- [ ] Test di `driveTo` su un punto noto.
- [ ] Verifica che l'heading finale sia quello atteso.
- [ ] Prova a batteria carica e scarica.
- [ ] Prova sulla superficie di gara, non solo sul tavolo.

---

## 14. Riferimenti

- goBILDA 4-Bar Odometry Pod (32 mm): https://www.gobilda.com/4-bar-odometry-pod-32mm-wheel
- goBILDA Swingarm Odometry Pod (48 mm): https://www.gobilda.com/swingarm-odometry-pod-48mm-wheel
- goBILDA Pinpoint, manuale: https://www.gobilda.com/content/user_manuals/3110-0002-0001%20User%20Guide.pdf
- gm0 — Dead Wheels: https://gm0.org/en/latest/docs/common-mechanisms/dead-wheels.html
- gm0 — Control System Electronics (porte encoder 0/3): https://gm0.org/en/latest/docs/software/adv-control-system/control-system-internals.html
- FTC Docs — Universal IMU Interface: https://ftc-docs.firstinspires.org/en/latest/programming_resources/imu/imu.html
- FTC Docs — Field Coordinate System Definition: https://ftc-docs.firstinspires.org/en/latest/game_specific_resources/field_coordinate_system/field-coordinate-system.html
- FTC — BIOBUZZ Competition Manual 2026‑27 (TU08/TU09): https://ftc-resources.firstinspires.org/ftc/game/manual?season=biobuzz
- FTC — BIOBUZZ Event Field Setup Guide 2026‑27: https://ftc-resources.firstinspires.org/ftc/archive/2027/field/eventfieldguide
- FTC Docs — AprilTag Clusters (BIOBUZZ HIVE): https://ftc-docs.firstinspires.org/en/latest/tech_tips/tech-tips/tech-tip-apriltag-clusters/tech-tip-apriltag-clusters.html
- Pedro Pathing — Coordinates: https://pedropathing.com/docs/pathing/reference/coordinates
- Road Runner — Dead Wheels: https://learnroadrunner.com/dead-wheels.html
- Sample nel repo: `FtcRobotController/src/main/java/org/firstinspires/ftc/robotcontroller/external/samples/SensorGoBildaPinpoint.java`
- Road Runner 1.0 — Tuning: https://rr.brott.dev/docs/v1-0/tuning/
- Road Runner 1.0 — Installazione: https://rr.brott.dev/docs/v1-0/installation
- Pedro Pathing — Two Wheel: https://pedropathing.com/docs/pathing/tuning/localization/two-wheel
- Pedro Pathing — Installazione: https://pedropathing.com/docs/pathing/installation

---

**In sintesi:** l'odometria a due pod è `due encoder + IMU + matematica + calibrazione`. Il codice è la parte facile; la precisione viene dal montaggio, dagli offset misurati e dai test. Il frame top-left `(0,0)` è solo una convenzione: l'importante è usarla ovunque. E ricorda: l'odometria non sa dove sei all'accensione; `setPose(x, y, heading)` è ciò che le dice da dove parte il robot. Con quello, può partire davvero da qualunque punto del campo.
