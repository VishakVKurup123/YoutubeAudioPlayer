package com.vvk.youtubeaudioplayer;

import java.util.HashMap;

/**
 * Created by developer on 12/2/17.
 */

public interface ExtractionEventListener {
    void extractionCompleted(HashMap<String, String> results);
}
