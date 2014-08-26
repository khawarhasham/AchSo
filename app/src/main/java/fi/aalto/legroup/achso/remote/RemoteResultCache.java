/*
 * Code contributed to the Learning Layers project
 * http://www.learning-layers.eu
 * Development is partly funded by the FP7 Programme of the European
 * Commission under
 * Grant Agreement FP7-ICT-318209.
 * Copyright (c) 2014, Aalto University.
 * For a list of contributors see the AUTHORS file at the top-level directory
 * of this distribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.aalto.legroup.achso.remote;

import android.util.Log;

import java.util.List;
import java.util.HashMap;


import fi.aalto.legroup.achso.database.SemanticVideo;

public class RemoteResultCache {
    static HashMap <Integer, List<SemanticVideo>> cache = new HashMap <Integer,
            List<SemanticVideo>>();
    static HashMap <Integer, Boolean> cache_has_result = new HashMap <Integer, Boolean>();
    static SemanticVideo selected_video = null;

    public static void clearCache(int page) {
        List<SemanticVideo> list = cache.get(page);
        if (list != null) {
            list.clear();
        }
        cache_has_result.put(page, false);
    }

    public static void setCached(int page, List<SemanticVideo> resultlist) {
        cache.put(page, resultlist);
        cache_has_result.put(page, true);
    }

    public static List<SemanticVideo> getCached(int page) {
        return cache.get(page);
    }

    public static boolean hasCached(int page) {
        if (cache_has_result.containsKey(page)) {
            return cache_has_result.get(page);
        } else {
            return false;
        }
    }

    /*
    * Give easy access to selected remote video, make sure that this is set before starting video
     * player
    * */
    public static SemanticVideo getSelectedVideo() {
        return selected_video;
    }

    public static void setSelectedVideo(SemanticVideo video) {
        selected_video = video;
    }

}