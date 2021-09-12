/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.espe.distribuidas.generadores.transacciones.model;

import java.math.BigDecimal;
import lombok.Data;

/**
 *
 * @author jdismael
 */

@Data
public class TransaccionRS {

    private String cuentaId;
    private String cuentaSalida;
    private String descripcion;
    private BigDecimal monto;
    private String tipo;
}
