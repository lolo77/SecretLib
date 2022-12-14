package com.secretlib.io.jpeg;

import com.secretlib.util.Log;

import java.util.Arrays;

public class HuffNode {

    private static final Log LOG = new Log(HuffNode.class);


    public int index = 0;
    public HuffNode[] children = new HuffNode[2];
    public Byte[] data = new Byte[2];

    public HuffNode() {
        Arrays.fill(children, null);
        Arrays.fill(data, null);
    }



    public static void dumpTree(HuffNode node, int level, String path) {
        int lvl = level;

        String tab = "";
        for (int i = 0; i < level; i++)
            tab += "   ";

        for (int b = 0; b < 2; b++) {
            if (node.data[b] != null) {
                LOG.debug("level " + lvl + tab + " node."+b+" = " + node.data[b] + " / path " + path + b);
            } else
            if (node.children[b] != null) {
                LOG.debug("level " + lvl + tab +  " node."+b+" = Subtree level " + (lvl+1) + " / path " + path + b);
                dumpTree(node.children[b], lvl+1, path + b);
            }
        }
    }
}
