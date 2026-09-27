import java.util.Random;

public class QuimicoSimulado implements Sensor {
    private String nombre;
    private double valorBase;
    private double deriva;
    private double tiempoPrecalentamiento; // En segundos
    private long tiempoInicio;
    private Random random;

    public QuimicoSimulado(String nombre, double valorBase, double tiempoPrecalentamiento) {
        this.nombre = nombre;
        this.valorBase = valorBase;
        this.deriva = 0.0;
        this.tiempoPrecalentamiento = tiempoPrecalentamiento;
        this.tiempoInicio = System.currentTimeMillis();
        this.random = new Random();
    }

    @Override
    public double leer() throws Exception {
        double tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicio) / 1000.0;
        
        // Simulación de precalentamiento (Lanza excepción durante los primeros segundos)
        if (tiempoTranscurrido < tiempoPrecalentamiento) {
            throw new RuntimeErrorException("Sensor aun calentando");
        }

        // Simulación de deriva lenta + ruido gaussiano
        this.deriva += random.nextGaussian() * 0.01;
        double valorActual = this.valorBase + this.deriva;
        
        return Math.round(valorActual * 100.0) / 100.0; // Redondeado a 2 decimales
    }

    @Override
    public String getNombre() {
        return this.nombre;
    }

    @Override
    public String getUnidad() {
        return "PPM";
    }

    // Excepción personalizada para simular fallo de runtime
    public static class RuntimeErrorException extends Exception {
        public RuntimeErrorException(String mensaje) {
            super(mensaje);
        }
    }
}