/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.search.archiveorg;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import com.frostwire.search.CrawlPagedWebSearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.util.JsonUtils;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ArchiveorgSearchPerformer extends CrawlPagedWebSearchPerformer<ArchiveorgSearchResult> {

    public ArchiveorgSearchPerformer(long token, String keywords, int timeout) {
        super(token, keywords, timeout, 1, 12);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://archive.org/advancedsearch.php?q="
                + encodedKeywords
                + "&fl[]=avg_rating&fl[]=call_number&fl[]=collection&fl[]=contributor&fl[]=coverage&fl[]=creator&fl[]=date&fl[]=description&fl[]=downloads&fl[]=foldoutcount&fl[]=format&fl[]=headerImage&fl[]=identifier&fl[]=imagecount&fl[]=language&fl[]=licenseurl&fl[]=mediatype&fl[]=month&fl[]=num_reviews&fl[]=oai_updatedate&fl[]=publicdate&fl[]=publisher&fl[]=rights&fl[]=scanningcentre&fl[]=source&fl[]=subject&fl[]=title&fl[]=type&fl[]=volume&fl[]=week&fl[]=year&sort[]=avg_rating+desc&sort[]=downloads+desc&sort[]=createdate+desc&rows=50&page=1&indent=yes&output=json";
    }

    @Override
    protected List<? extends SearchResult> searchPage(String page) {
        List<SearchResult> result = new LinkedList<SearchResult>();

        ArchiveorgResponse response = JsonUtils.toObject(page, ArchiveorgResponse.class);

        for (ArchiveorgItem item : response.response.docs) {
            if (!isStopped()) {
                ArchiveorgSearchResult sr = new ArchiveorgSearchResult(item);
                result.add(sr);
            }
        }

        return result;
    }

    @Override
    protected String getCrawlUrl(ArchiveorgSearchResult sr) {
        return "http://archive.org/details/" + sr.getItem().identifier + "?output=json";
    }

    @Override
    protected List<? extends SearchResult> crawlResult(ArchiveorgSearchResult sr, byte[] data) throws Exception {
        List<ArchiveorgCrawledSearchResult> list = new LinkedList<ArchiveorgCrawledSearchResult>();

        String json = new String(data, "UTF-8");
        JSONObject obj = new JSONObject(json);
        JSONObject files = obj.getJSONObject("files");

        @SuppressWarnings("unchecked")
        Iterator<String> it = files.keys();

        while (it.hasNext() && !isStopped()) {
            String name = it.next();
            ArchiveorgFile file = JsonUtils.toObject(files.getJSONObject(name).toString(), ArchiveorgFile.class);
            if (filter(file)) {
                list.add(new ArchiveorgCrawledSearchResult(sr, cleanName(name), file));
            }
        }

        return list;
    }

    private String cleanName(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        return name;
    }

    private boolean filter(ArchiveorgFile file) {
        if (file.format != null && file.format.equalsIgnoreCase("metadata")) {
            return false;
        }

        return true;
    }
}
