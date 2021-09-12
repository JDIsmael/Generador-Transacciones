/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.espe.distribuidas.generadores.transacciones.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 *
 * @author jdismael
 */
@Data
public class Cuenta {
    private String cuentaId;
    private BigDecimal saldoDisponible;
    
    
    private List<Transaccion> transaccion;
}
