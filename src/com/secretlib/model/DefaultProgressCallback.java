package com.secretlib.model;

import com.secretlib.util.Log;

public class DefaultProgressCallback implements IProgressCallback {
    private static final Log LOG = new Log(DefaultProgressCallback.class);

    @Override
    public void update(ProgressMessage msg) {
        if (LOG.isDebug()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Step : " + ((msg.getStep() != null) ? msg.getStep() : "") + "; " + (int)(msg.getProgress()*100.0) + "%");
            if ((msg.getNbBitsUsed() > 0) && (msg.getNbBitsCapacity() > 0)) {
                sb.append(" (used/total : " + msg.getNbBitsUsed() +"/" + msg.getNbBitsCapacity() + " ; Changed : " + msg.getNbBitsChanged() + ")");
            }
            LOG.debug(sb.toString());
        }
    }
}
