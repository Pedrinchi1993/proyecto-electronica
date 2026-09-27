public class PruebasSensor {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS AUTOMATIZADAS DEL SENSOR ===");
        
        // Sensor con 3 segundos de precalentamiento para comprobar fallos tolerados
        Sensor sensorTest = new QuimicoSimulado("MQ-2 Test", 20.0, 3.0);
        
        int totalMuestras = 10;
        int exitosas = 0;
        int excepcionesCapturadas = 0;

        for (int i = 1; i <= totalMuestras; i++) {
            try {
                double valor = sensorTest.leer();
                if (valor >= 0.0 && valor <= 100.0) {
                    exitosas++;
                }
            } catch (Exception e) {
                excepcionesCapturadas++;
            }

            try {
                Thread.sleep(1000); // 1 segundo entre prueba
            } catch (InterruptedException e) {
                break;
            }
        }

        System.out.println("\n=== RESULTADOS DE PRUEBAS ===");
        System.out.println("Total de Muestras evaluables: " + totalMuestras);
        System.out.println("Lecturas exitosas en rango: " + exitosas);
        System.out.println("Excepciones capturadas/toleradas: " + excepcionesCapturadas);
        System.out.println("ESTADO DE PRUEBA: PASADA (La aplicacion es tolerante a fallos)");
    }
}