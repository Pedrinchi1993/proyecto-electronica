# Sistema de Adquisición y Simulación de Sensor Químico MOS (MQ-2)
> **Asignatura:** Sistemas Digitales III  
> **Plataforma Objetivo:** Raspberry Pi (Raspbian / Raspberry Pi OS)  
> **Lenguaje de Programación:** Java (JDK 11+)  

---

## 1. Descripción del Proyecto

Este proyecto implementa una aplicación completa para la adquisición, registro y visualización en tiempo real de datos provenientes de un **sensor de gas/humo de la serie MQ (MQ-2)**. 

Siguiendo la metodología de cinco fases exigida en el curso, el proyecto desacopla completamente la lógica de adquisición y procesamiento de datos del hardware físico mediante una **clase simulada fiel al comportamiento físico-químico del sensor**. De esta manera, el sistema funciona de forma autónoma y queda listo para recibir el hardware real sin necesidad de reescribir el código base.

---

## 2. Ficha Técnica del Sensor (Fase 1)

* **Sensor:** MQ-2 (LPG, i-butano, propano, metano, alcohol, humo).
* **Grupo de Simulación:** Químico MOS (Óxido Metálico Semiconductor).
* **Magnitud Medida:** Concentración de gas en partes por millón (**PPM**).
* **Voltaje de Operación:** 5.0 V DC.
* **Interfaz Análoga:** Tensión de salida proporcional a la concentración.
* **Requerimiento de Hardware en Raspberry Pi:** Requiere un conversor analógico-digital (ADC) externo como el **MCP3008** con comunicación vía bus SPI, dado que la Raspberry Pi no posee entradas analógicas nativas.
* **Comportamiento Físico Característico:**
  1. **Precalentamiento:** Requiere tiempo de estabilización térmica antes de entregar lecturas válidas.
  2. **Deriva Lenta:** Variación continua y gradual del valor base por efecto de la temperatura ambiente y envejecimiento.
  3. **Ruido Gaussiano:** Fluctuaciones estadísticas en la medición.

---

## 3. Arquitectura del Software (Separación de Capas)

El proyecto está diseñado bajo principios de programación orientada a objetos (POO) garantizando el aislamiento del hardware:

```text
src/
├── Sensor.java              # Interfaz genérica del hardware (Capa de Abstracción)
├── QuimicoSimulado.java     # Modelo matemático que simula el sensor MQ-2
├── GraficaSensor.java       # Interfaz gráfica en Java Swing con ventana acotada
├── App.java                 # Módulo principal y gestor de hilos de ejecución
└── PruebasSensor.java       # Pruebas unitarias y de robustez
```

---

## 4. Esquema de Conexión Física (Raspberry Pi + MCP3008 + MQ-2)

Aun cuando el sistema opera en modo simulado, se define la topología de conexión para el despliegue con hardware real:

```text
  +-----------------------+              +-----------------------+
  |  Raspberry Pi 3 / 4   |              |   ADC MCP3008 (SPI)   |
  |                       |              |                       |
  | Pin 1  (3.3V) --------+--------------+ VDD, VREF             |
  | Pin 6  (GND) ---------+--------------+ AGND, DGND            |
  | Pin 19 (MOSI) --------+--------------+ DIN                   |
  | Pin 21 (MISO) --------+--------------+ DOUT                  |
  | Pin 23 (SCLK) --------+--------------+ CLK                   |
  | Pin 24 (CE0) ---------+--------------+ CS/SHDN               |
  +-----------------------+              |                       |
                                         | CH0 <---+             |
                                         +---------|-------------+
                                                   |
                                         +---------|-------------+
                                         |  Sensor MQ-2 (Real)   |
                                         |                       |
                                         | VCC -----> 5V         |
                                         | GND -----> GND        |
                                         | AOUT ----> CH0 (MCP)  |
                                         +-----------------------+
```

---

## 5. Instrucciones de Compilación y Ejecución

### Requisitos Previos
* Tener instalado el JDK (Java Development Kit) 11 o superior.
* Verificable en la terminal mediante: `java -version` y `javac -version`.

### Compilación desde la Terminal
Ubícate en la raíz del proyecto y ejecuta:

```bash
javac -d bin src/*.java
```

### Ejecución de la Aplicación
Para iniciar el sistema de muestreo, registro en CSV y gráfica Swing:

```bash
java -cp bin App
```

### Ejecución de las Pruebas Unitarias
Para ejecutar la batería de pruebas automatizadas:

```bash
java -cp bin PruebasSensor
```

---

## 6. Despliegue como Servicio del Sistema (`systemd`)

Para garantizar que la aplicación arranque de manera automática al encender la Raspberry Pi y se reinicie automáticamente ante eventuales fallos, se configura como un servicio de `systemd`.

1. Crear el archivo de servicio `/etc/systemd/system/mq2_sensor.service`:

```ini
[Unit]
Description=Servicio de Monitoreo y Simulación de Sensor MQ-2
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/simulador-sensor
ExecStart=/usr/bin/java -cp /home/pi/simulador-sensor/bin App
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

2. Activar y arrancar el servicio en el sistema:

```bash
sudo systemctl daemon-reload
sudo systemctl enable mq2_sensor.service
sudo systemctl start mq2_sensor.service
```

3. Verificar el estado del servicio:

```bash
sudo systemctl status mq2_sensor.service
```

---

## 7. Estructura del Archivo de Registro (`registro_mq2.csv`)

El programa genera automáticamente un archivo `.csv` donde persiste de manera continua las lecturas:

```csv
Timestamp,Valor_PPM,Estado
1700000000000,0.0,PRECALENTANDO
1700000001000,0.0,PRECALENTANDO
1700000011000,20.15,OK
1700000012000,20.18,OK
```

---

## 8. Decisiones de Diseño y Robustez
* **Tolerancia a Fallos:** La excepción por precalentamiento (`RuntimeException`) es capturada dentro del bucle principal de muestreo, impidiendo la caída prematura del programa y registrando el estado en el CSV.
* **Control de Memoria:** La interfaz gráfica maneja una cola acotada de muestras ($N=20$), garantizando un consumo de memoria RAM constante a lo largo del tiempo.