package com.nisovin.magicspells.power;

public class Power extends Number {
    protected float amplifier;

    public Power(float amplifier) {
        this.amplifier = amplifier;
    }

    public final void setAmplifier(float amplifier) {
        this.amplifier = amplifier;
    }

    @Override
    public final int intValue() {
        return (int) amplifier;
    }

    @Override
    public final long longValue() {
        return (long) amplifier;
    }

    @Override
    public final float floatValue() {
        return amplifier;
    }

    @Override
    public final double doubleValue() {
        return amplifier;
    }

    public Power plus(Power power) {
        amplifier += power.amplifier;
        return this;
    }
    public Power minus(Power power) {
        amplifier -= power.amplifier;
        return this;
    }
    public Power multiply(Power power) {
        amplifier *= power.amplifier;
        return this;
    }
    public Power divide(Power power) {
        amplifier /= power.amplifier;
        return this;
    }
}
