package com.secretlib.io.jpeg;

public class Component {
    public int h = 0;
    public int v = 0;
    public int qId = 0;
    public int blocksPerLine = 0;
    public int blocksPerColumn = 0;
    public int pred = 0;
    public int huffmanTableSpec = 0;

    public HuffNode huffmanTableAC = null;
    public HuffNode huffmanTableDC = null;

    public int[][][] blocks = null; // [y][x][0..63]
}
