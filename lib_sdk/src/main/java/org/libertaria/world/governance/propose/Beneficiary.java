package org.libertaria.world.governance.propose;

import java.io.Serializable;

/**
 * Created by mati on 09/01/17.
 */
public class Beneficiary implements Serializable{

    private String address;
    private long amount;

    public Beneficiary() {
    }

    public Beneficiary(String address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public long getAmount() {
        return amount;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
