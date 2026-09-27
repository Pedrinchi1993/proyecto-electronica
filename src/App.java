import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class App extends JFrame {
    private Sensor sensorMQ2;
    private List<Double> historialLecturas;
    private final int MAX_MUESTRAS = 30; // Ventana acotada
    private JPanel panelGrafica;
    private JLabel lblValorActual;
    private JLabel lblEstadoActual;
    private String archivoCSV = "registro_mq2.csv";

    public App() {
        // Sensor MQ-2 con 3 segundos de precalentamiento para demostración
        this.sensorMQ2 = new QuimicoSimulado("MQ-2", 20.0, 3.0);
        this.historialLecturas = new ArrayList<>();

        // Configuración de la Ventana Principal
        setTitle("Sistema de Monitoreo - Sensor MQ-2 (Gas/Humo)");
        setSize(750, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. Panel Superior (Tarjeta de Estado)
        JPanel panelSuperior = new JPanel(new GridLayout(1, 2, 10, 10));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Estado del Sensor en Tiempo Real"));
        panelSuperior.setBackground(new Color(245, 247, 250));

        lblValorActual = new JLabel("Valor: -- PPM", SwingConstants.CENTER);
        lblValorActual.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblValorActual.setForeground(new Color(30, 41, 59));

        lblEstadoActual = new JLabel("Estado: INICIANDO", SwingConstants.CENTER);
        lblEstadoActual.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblEstadoActual.setForeground(Color.GRAY);

        panelSuperior.add(lblValorActual);
        panelSuperior.add(lblEstadoActual);
        add(panelSuperior, BorderLayout.NORTH);

        // 2. Panel Central (Gráfica Personalizada)
        panelGrafica = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                dibujarGraficaAvanzada(g);
            }
        };
        panelGrafica.setBackground(Color.WHITE);
        panelGrafica.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panelGrafica, BorderLayout.CENTER);

        // Crear encabezado en el CSV si no existe
        inicializarCSV();
    }

    private void inicializarCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoCSV, false))) {
            pw.println("Fecha_Hora,Valor_PPM,Estado");
        } catch (Exception e) {
            System.err.println("Error al inicializar CSV: " + e.getMessage());
        }
    }

    public void iniciarMuestreo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        new Thread(() -> {
            while (true) {
                String timestamp = sdf.format(new Date());
                double valor = 0.0;
                String estado = "OK";

                try {
                    valor = sensorMQ2.leer();
                    System.out.printf("[%s] Lectura exitosa: %.2f %s%n", 
                            timestamp, valor, sensorMQ2.getUnidad());

                    double finalValor = valor;
                    SwingUtilities.invokeLater(() -> {
                        lblValorActual.setText(String.format("Valor: %.2f PPM", finalValor));
                        lblEstadoActual.setText("Estado: NORMAL (OK)");
                        lblEstadoActual.setForeground(new Color(22, 101, 52)); // Verde
                    });

                    synchronized (historialLecturas) {
                        if (historialLecturas.size() >= MAX_MUESTRAS) {
                            historialLecturas.remove(0); // Mantiene ventana flotante acotada
                        }
                        historialLecturas.add(valor);
                    }
                } catch (Exception e) {
                    estado = "PRECALENTANDO";
                    System.out.printf("[%s] Advertencia: %s%n", timestamp, e.getMessage());

                    SwingUtilities.invokeLater(() -> {
                        lblValorActual.setText("Valor: -- PPM");
                        lblEstadoActual.setText("Estado: PRECALENTANDO");
                        lblEstadoActual.setForeground(new Color(194, 65, 12)); // Naranja
                    });
                }

                // Guardar en archivo CSV con formato legible
                guardarEnCSV(timestamp, valor, estado);

                // Repintar gráfica
                panelGrafica.repaint();

                try {
                    Thread.sleep(1000); // 1 Muestra cada 1 segundo
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void guardarEnCSV(String timestamp, double valor, String estado) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoCSV, true))) {
            pw.printf("%s,%.2f,%s%n", timestamp, valor, estado);
        } catch (Exception e) {
            System.err.println("Error al escribir en CSV: " + e.getMessage());
        }
    }

    private void dibujarGraficaAvanzada(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ancho = panelGrafica.getWidth();
        int alto = panelGrafica.getHeight();
        int margenIzq = 60;
        int margenAbaixo = 40;
        int margenSup = 20;
        int margenDer = 30;

        int anchoGrafica = ancho - margenIzq - margenDer;
        int altoGrafica = alto - margenSup - margenAbaixo;

        // 1. Dibujar Rejilla de Fondo (Grid)
        g2.setColor(new Color(230, 235, 240));
        int numDivisionesY = 5;
        for (int i = 0; i <= numDivisionesY; i++) {
            int y = margenSup + (altoGrafica * i / numDivisionesY);
            g2.drawLine(margenIzq, y, ancho - margenDer, y);
        }

        // 2. Determinar Escala en Y (Eje de Concentración en PPM)
        double minVal = 15.0;
        double maxVal = 25.0;

        synchronized (historialLecturas) {
            if (!historialLecturas.isEmpty()) {
                for (double val : historialLecturas) {
                    if (val < minVal) minVal = Math.floor(val) - 1.0;
                    if (val > maxVal) maxVal = Math.ceil(val) + 1.0;
                }
            }
        }

        // 3. Dibujar Etiquetas del Eje Y
        g2.setColor(new Color(100, 116, 139));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i <= numDivisionesY; i++) {
            double valPPM = maxVal - (i * (maxVal - minVal) / numDivisionesY);
            int y = margenSup + (altoGrafica * i / numDivisionesY);
            g2.drawString(String.format("%.1f", valPPM), 10, y + 4);
        }

        // 4. Dibujar Ejes Principales
        g2.setColor(new Color(51, 65, 85));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(margenIzq, alto - margenAbaixo, ancho - margenDer, alto - margenAbaixo); // Eje X
        g2.drawLine(margenIzq, margenSup, margenIzq, alto - margenAbaixo); // Eje Y

        // Título del Eje Y
        g2.drawString("PPM", 15, margenSup - 5);

        // 5. Trazar Serie de Datos (Línea y Puntos)
        synchronized (historialLecturas) {
            int numPuntos = historialLecturas.size();
            if (numPuntos < 1) {
                g2.setColor(Color.GRAY);
                g2.drawString("Esperando lecturas de estabilización...", ancho / 2 - 100, alto / 2);
                return;
            }

            int pasoX = anchoGrafica / (MAX_MUESTRAS - 1);

            int[] xPuntos = new int[numPuntos];
            int[] yPuntos = new int[numPuntos];

            for (int i = 0; i < numPuntos; i++) {
                xPuntos[i] = margenIzq + i * pasoX;
                double val = historialLecturas.get(i);
                yPuntos[i] = (int) (alto - margenAbaixo - ((val - minVal) / (maxVal - minVal)) * altoGrafica);
            }

            // Dibujar Línea de Conexión
            g2.setColor(new Color(37, 99, 235)); // Azul
            g2.setStroke(new BasicStroke(2.5f));
            for (int i = 0; i < numPuntos - 1; i++) {
                g2.drawLine(xPuntos[i], yPuntos[i], xPuntos[i + 1], yPuntos[i + 1]);
            }

            // Dibujar Puntos de Muestra
            g2.setColor(new Color(29, 78, 216));
            for (int i = 0; i < numPuntos; i++) {
                g2.fillOval(xPuntos[i] - 4, yPuntos[i] - 4, 8, 8);
            }

            // Destacar el Último Punto
            int ultX = xPuntos[numPuntos - 1];
            int ultY = yPuntos[numPuntos - 1];
            g2.setColor(new Color(220, 38, 38)); // Rojo
            g2.fillOval(ultX - 5, ultY - 5, 10, 10);
            g2.drawString(String.format("%.2f", historialLecturas.get(numPuntos - 1)), ultX - 15, ultY - 10);
        }

        // Título Eje X
        g2.setColor(new Color(100, 116, 139));
        g2.drawString("Muestras (Ventana acotada de " + MAX_MUESTRAS + " seg)", ancho / 2 - 80, alto - 10);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            App app = new App();
            app.setVisible(true);
            app.iniciarMuestreo();
        });
    }
}