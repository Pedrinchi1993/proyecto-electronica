import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraficaSensor extends JPanel {
    private final List<Double> historial;
    private final int maxMuestras;
    private String estadoMensaje = "Iniciando...";
    private Color estadoColor = Color.GRAY;

    // Constructor por defecto (Ventana de 20 muestras)
    public GraficaSensor() {
        this(20);
    }

    public GraficaSensor(int maxMuestras) {
        this.maxMuestras = maxMuestras;
        this.historial = new ArrayList<>();
        setBackground(new Color(245, 247, 250));
    }

    // Método para mantener compatibilidad
    public synchronized void agregarDato(double valor) {
        agregarMuestra(valor, "Operando OK", new Color(46, 125, 50));
    }

    public synchronized void agregarMuestra(double valor, String mensaje, Color color) {
        if (historial.size() >= maxMuestras) {
            historial.remove(0); // Limita las muestras en memoria
        }
        historial.add(valor);
        this.estadoMensaje = mensaje;
        this.estadoColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padding = 60;
        int width = getWidth();
        int height = getHeight();

        // Título e Indicador
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Monitoreo de Gas MQ-2 (Simulado)", padding, 35);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(estadoColor);
        g2.drawString("Estado: " + estadoMensaje, width - 220, 35);

        if (historial.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Esperando primeras lecturas...", width / 2 - 90, height / 2);
            return;
        }

        // Escala en Eje Y
        double minVal = Collections.min(historial);
        double maxVal = Collections.max(historial);
        if (maxVal == minVal) {
            maxVal += 5.0;
            minVal = Math.max(0, minVal - 5.0);
        }

        int graphW = width - (2 * padding);
        int graphH = height - (2 * padding);

        // Cuadrícula Eje Y
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        int numDivs = 5;
        for (int i = 0; i <= numDivs; i++) {
            int y = height - padding - (i * graphH / numDivs);
            double val = minVal + (i * (maxVal - minVal) / numDivs);

            g2.setColor(new Color(220, 224, 230));
            g2.drawLine(padding, y, width - padding, y);

            g2.setColor(Color.GRAY);
            g2.drawString(String.format("%.1f PPM", val), 10, y + 4);
        }

        // Ejes
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(padding, height - padding, width - padding, height - padding);
        g2.drawLine(padding, padding, padding, height - padding);

        // Trazado de Serie
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(new Color(30, 136, 229));

        int n = historial.size();
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];

        for (int i = 0; i < n; i++) {
            xPoints[i] = padding + (i * graphW / Math.max(1, maxMuestras - 1));
            yPoints[i] = height - padding - (int) ((historial.get(i) - minVal) / (maxVal - minVal) * graphH);
        }

        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }

        for (int i = 0; i < n; i++) {
            g2.setColor(new Color(21, 101, 192));
            g2.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);

            if (i == n - 1) {
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.drawString(String.format("%.2f PPM", historial.get(i)), xPoints[i] - 15, yPoints[i] - 10);
            }
        }
    }
}