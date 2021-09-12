/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.espe.distribuidas.generadores.transacciones.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ec.edu.espe.distribuidas.generadores.transacciones.config.ApplicationValues;
import ec.edu.espe.distribuidas.generadores.transacciones.model.Cuenta;
import ec.edu.espe.distribuidas.generadores.transacciones.model.TransaccionRS;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Admin
 */
@Slf4j
public class GeneracionTransacciones implements Tasklet, StepExecutionListener {

    private final ApplicationValues applicationValues;
    private final String DESCRIPCIONES_FILE = "descripciones.txt";
    private RestTemplate restTemplate;

    private Integer porcentajeClientes;
    private Integer cantidadPorCliente;

    private String tipoTrasacciones[] = {"DEP", "RET", "TRO"};

    private List<String> descripciones;

    private List<String> ids = new ArrayList<>();

    private List<Cuenta> cuentas;

    public GeneracionTransacciones(ApplicationValues applicationValues) {
        this.applicationValues = applicationValues;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void beforeStep(StepExecution se) {
        try {

            //Path fileDescripciones = Paths.get(this.applicationValues.getDataPath() + this.DESCRIPCIONES_FILE);
            //this.descripciones = Files.readAllLines(fileDescripciones);
            ExecutionContext sc = se.getJobExecution().getExecutionContext();
            this.porcentajeClientes = (Integer) sc.get("porcentajeClientes");
            this.cantidadPorCliente = (Integer) sc.get("cantidadPorCliente");

            log.info("{}", this.porcentajeClientes);
            log.info("{} transacciones por cliente", this.cantidadPorCliente);

            JsonNode respuesta = restTemplate.getForObject("http://52.146.55.208:8004/api/clienteProductoPasivo", JsonNode.class);

            for (JsonNode r : respuesta) {
                ObjectNode object = (ObjectNode) r;
                object.remove("productoPasivo");
                object.remove("estado");
                object.remove("fechaCreacion");
                object.remove("saldoContable");
            }

            ObjectMapper mapper = new ObjectMapper();

            this.cuentas = mapper.convertValue(
                    respuesta,
                    new TypeReference<ArrayList<Cuenta>>() {
            }
            );

            this.cuentas = this.cuentas.stream()
                    .filter(c -> c.getTransaccion().isEmpty())
                    .collect(Collectors.toList());

            this.cuentas.forEach(c -> ids.add(c.getCuentaId()));

            Integer cantidadCuentas = (this.cuentas.size() * this.porcentajeClientes) / 100;

            log.info("ids : {}", ids);

            log.info("{} cuentas antes", this.cuentas.size());
            this.cuentas = this.cuentas.subList(0, cantidadCuentas);
            log.info("{} cuentas despues", this.cuentas.size());

        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {

        TransaccionRS transaccion;
        Random rand = new Random();
        Integer opcion;
        BigDecimal saldoDisponible;
        BigDecimal dinero;
        
        for (Cuenta cuenta : this.cuentas) {

            transaccion = new TransaccionRS();
            transaccion.setCuentaId(cuenta.getCuentaId());
            log.info("\nCuenta: {}",transaccion.getCuentaId());
            
            for (int i = 0; i < this.cantidadPorCliente; i++) {
                saldoDisponible = cuenta.getSaldoDisponible();
                transaccion.setCuentaSalida(null);
                log.info("transaccion {}",i+1);
                log.info("Saldo anterior: {}",saldoDisponible);
                opcion = saldoDisponible.compareTo(new BigDecimal("10.00")) == -1 ? 0 : rand.nextInt(3);
                switch (opcion) {
                    case 0:
                        dinero = new BigDecimal(rand.nextDouble() * 1000, MathContext.DECIMAL32).setScale(2, RoundingMode.HALF_EVEN);
                        transaccion.setMonto(dinero);
                        transaccion.setDescripcion("Deposito");
                        transaccion.setTipo("DEP");
                        cuenta.setSaldoDisponible(saldoDisponible.add(dinero));
                        break;
                    case 1:
                        dinero = new BigDecimal(2 + ( saldoDisponible.intValue() - 2 ) * rand.nextDouble(), MathContext.DECIMAL32).setScale(2, RoundingMode.HALF_EVEN);
                        transaccion.setMonto(dinero);
                        transaccion.setDescripcion("Retiro");
                        transaccion.setTipo("RET");
                        
                        cuenta.setSaldoDisponible(saldoDisponible.subtract(dinero));
                        
                        break;
                    case 2:
                        dinero = new BigDecimal(2 + ( saldoDisponible.intValue() - 2 ) * rand.nextDouble(), MathContext.DECIMAL32).setScale(2, RoundingMode.HALF_EVEN);
                        transaccion.setMonto(dinero);
                        transaccion.setDescripcion("Transferencia");
                        transaccion.setTipo("TRO");
                        transaccion.setCuentaSalida(this.getCuentaSalidaId(transaccion.getCuentaId()));
                        cuenta.setSaldoDisponible(saldoDisponible.subtract(dinero));
                        break;
                }
                
                log.info("{}",transaccion);
                restTemplate.postForObject("http://52.146.55.208:8004/api/transaccion", transaccion, String.class);
            }
        }
        return RepeatStatus.FINISHED;
    }

    public String getCuentaSalidaId(String cuentaId){
        String id = "";
        Random r = new Random();
        for(int i=0;i<this.ids.size();i++){
            id = this.ids.get(r.nextInt(this.ids.size()));
            if(!id.equals(cuentaId))
                break;
        }
        return id;
    }
    
    @Override
    public ExitStatus afterStep(StepExecution se) {
        return ExitStatus.COMPLETED;
    }

}
