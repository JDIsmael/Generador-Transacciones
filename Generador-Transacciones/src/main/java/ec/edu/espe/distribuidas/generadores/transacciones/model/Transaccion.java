/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.espe.distribuidas.generadores.transacciones.model;

import lombok.Data;

/**
 *
 * @author jdismael
 */
@Data
public class Transaccion {
    private String codTransaccion;
    private String cuentaSalida;
    private String descripcion;
    private String tipo;
    private String monto;
    private String saldoAnterior;
    private String saldoActual;
    private String fecha;
}
