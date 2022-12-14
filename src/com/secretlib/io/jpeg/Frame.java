package com.secretlib.io.jpeg;

import java.util.ArrayList;
import java.util.HashMap;

public class Frame {
    public boolean extended = false;
    public boolean progressive = false;
    public int precision = 0;
    public int scanLines = 0;
    public int samplesPerLine = 0;
    public int maxH = 0;
    public int maxV = 0;
    public int mcusPerLine = 0;
    public int mcusPerColumn = 0;
    public int spectralStart = 0;
    public int spectralEnd = 63;

    public HashMap<Integer, HuffmanTable> htables = new HashMap<>();
    public HashMap<Integer, QtzTable> qTables = new HashMap<>();
    public HashMap<Integer, Component> comps = new HashMap<>();
    public ArrayList<Integer> compOrder = new ArrayList<>();
    public ArrayList<byte[]> apps = new ArrayList<>();

}
