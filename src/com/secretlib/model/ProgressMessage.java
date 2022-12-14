package com.secretlib.model;

import java.util.Arrays;

/**
 * @author Florent FRADET
 * <p>
 * A progression rate & message
 */
public class ProgressMessage {

    private double progress;
    private ProgressStepEnum step;
    private int nbBitsTotals[] = new int[8];
    private int nbBitsCapacity = 0;
    private int nbBitsUsed = 0;
    private int nbBitsChanged = 0;

    public ProgressMessage(ProgressStepEnum step, double progress) {
        Arrays.fill(nbBitsTotals, 0);
        this.step = step;
        this.progress = progress;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public ProgressStepEnum getStep() {
        return step;
    }

    public void setStep(ProgressStepEnum step) {
        this.step = step;
    }

    public int getNbBitsTotalSum() {
        int nb = 0;
        for (int i = 0; i < 8; i++) {
            nb += nbBitsTotals[i];
        }
        return nb;
    }

    public int getNbBitsTotal(int bit) {
        if ((bit >= 0) && (bit <= 7))
            return nbBitsTotals[bit];

        return -1;
    }

    public void setNbBitsTotal(int nbBitsTotal, int bit) {
        if ((bit >= 0) && (bit <= 7))
            this.nbBitsTotals[bit] = nbBitsTotal;
    }

    public int[] getNbBitsTotals() {
        return nbBitsTotals;
    }

    public void setNbBitsTotals(int[] nbBitsTotal) {
        for (int i = 0; i < 8; i++) {
            this.nbBitsTotals[i] = nbBitsTotal[i];
        }
    }

    public int getNbBitsCapacity() {
        return nbBitsCapacity;
    }

    public void setNbBitsCapacity(int nbBitsCapacity) {
        this.nbBitsCapacity = nbBitsCapacity;
    }

    public int getNbBitsUsed() {
        return nbBitsUsed;
    }

    public void setNbBitsUsed(int nbBitsUsed) {
        this.nbBitsUsed = nbBitsUsed;
    }

    public int getNbBitsChanged() {
        return nbBitsChanged;
    }

    public void setNbBitsChanged(int nbBitsChanged) {
        this.nbBitsChanged = nbBitsChanged;
    }
}
