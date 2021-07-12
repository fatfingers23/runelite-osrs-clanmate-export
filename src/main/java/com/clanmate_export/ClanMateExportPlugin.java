/*
 * Copyright (c) 2021, Bailey Townsend <baileytownsend2323@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.clanmate_export;

import com.google.gson.Gson;
import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.runelite.http.api.RuneLiteAPI.JSON;

@Slf4j
@PluginDescriptor(
        name = "Clanmate Export"
)
public class ClanMateExportPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClanMateExportConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClanMateExportOverlay overlay;

    private static final Gson GSON = RuneLiteAPI.GSON;


    /**
     * The clan members, scraped from your clan setup widget
     */
    private List<ClanMemberMap> clanMembers = null;


    /**
     * The number of runescape players in a clan
     */
    private int clanMemberCount;

    /**
     * Name of the runescape clan
     */
    private String clanName;

    /**
     * @return the clan members from the clan roster widget
     */
    public @Nullable
    List<ClanMemberMap> getClanMembers() {
        return this.clanMembers;
    }


    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
    }


    @Provides
    ClanMateExportConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ClanMateExportConfig.class);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widget) {

        if (widget.getGroupId() == 690) {
            overlay.update(ClanMateExportOverlay.WhatToShow.OPEN_MEMBERS_SCREEN);
        }

        //693 is the member list group inside of clan settings
        if (widget.getGroupId() == 693) {
            if (this.client.getWidget(693, 9) == null) {
                this.clanMembers = null;
            } else {
                scrapeMembers();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        //Update the overlay if the clan setup widget is visible on screen
        if (this.client.getWidget(707, 0) != null) {
            this.setClanInfo();
        }

        if (this.client.getWidget(690, 0) == null) {
            if(this.client.getWidget(693, 0) == null){
                overlay.update(ClanMateExportOverlay.WhatToShow.REMOVE);
            }
        }
    }

    /**
     * Sets clan info
     */
    private void setClanInfo() {
        //Gets and sets clan count
        Widget memberCounterWidget = this.client.getWidget(701, 3);
        if (memberCounterWidget != null) {
            if (memberCounterWidget.getText() != null) {
                String clanSizeText = Text.removeTags(memberCounterWidget.getText());
                if (clanSizeText.contains("Size:")) {
                    this.clanMemberCount = Integer.parseInt(clanSizeText.replace("Size: ", ""));
                }


            }
        }
        //Gets and sets clan name
        Widget clanNameWidget = this.client.getWidget(701, 1);
        if (clanNameWidget != null) {
            this.clanName = Text.removeTags(clanNameWidget.getText());
        }
    }

    /**
     * Subroutine - Update our memory of clan members and their ranks for
     * clan setup
     */
    public void scrapeMembers() {
        if (this.clanMembers == null) {
            this.clanMembers = new ArrayList<>();
        }
        this.clanMembers.clear();

        //Checks to set up scraping
        Widget clanMemberNamesWidget = this.client.getWidget(693, 10);
        Widget rankWidget = this.client.getWidget(693, 11);
        Widget joinedWidget = this.client.getWidget(693, 13);

        Widget[] leftColumnName = Objects.requireNonNull(this.client.getWidget(693, 7)).getChildren();
        if (leftColumnName != null) {
            if (!leftColumnName[4].getText().equals("Rank")) {
                overlay.update(ClanMateExportOverlay.WhatToShow.CHECK_COLUMNS_RANKED);
                return;
            }
        }

        Widget[] rightColumnName = Objects.requireNonNull(this.client.getWidget(693, 8)).getChildren();

        if (rightColumnName != null) {
            if (!rightColumnName[4].getText().equals("Joined")) {
                overlay.update(ClanMateExportOverlay.WhatToShow.CHECK_COLUMNS_JOINED);
                return;
            }

        }

        if (clanMemberNamesWidget == null || rankWidget == null || joinedWidget == null) {
            return;
        }
        Widget[] clanMemberNamesWidgetValues = clanMemberNamesWidget.getChildren();
        Widget[] rankWidgetValues = rankWidget.getChildren();
        Widget[] joinedWidgetValues = joinedWidget.getChildren();

        if (clanMemberNamesWidgetValues == null || rankWidgetValues == null || joinedWidgetValues == null) {
            return;
        }
        //Scrape all clan members

        int lastSuccessfulRsnIndex = 0;
        int otherColumnsPositions = 0;
        for (int i = 0; i < clanMemberNamesWidgetValues.length; i++) {
            int valueOfRsnToGet;
            if (i == 0) {
                valueOfRsnToGet = 1;
            } else {
                valueOfRsnToGet = lastSuccessfulRsnIndex + 3;
            }
            boolean inBounds = (valueOfRsnToGet >= 0) && (valueOfRsnToGet < clanMemberNamesWidgetValues.length);
            if (inBounds) {
                int otherColumnsIndex = otherColumnsPositions + this.clanMemberCount;
                String rsn = Text.removeTags(clanMemberNamesWidgetValues[valueOfRsnToGet].getText());
                String rank = Text.removeTags(rankWidgetValues[otherColumnsIndex].getText());
                String joinedDate = Text.removeTags(joinedWidgetValues[otherColumnsIndex].getText());
                ClanMemberMap clanMember = new ClanMemberMap(rsn, rank, joinedDate);
                this.clanMembers.add(clanMember);
                lastSuccessfulRsnIndex = valueOfRsnToGet;
                otherColumnsPositions++;
            }
        }


        if (this.config.exportToClipBoard()) {
            String clipBoardString = "";

            switch (this.config.getDataExportFormat()) {
                case JSON:
                    clipBoardString = toJson(this.clanMembers);
                    break;
                case CSV:
                    clipBoardString = toCSV(this.clanMembers);
                    break;

            }

            this.clanMembersToClipBoard(clipBoardString);
            overlay.update(ClanMateExportOverlay.WhatToShow.SUCCESS);
        }

        if (this.config.getSendWebRequest()) {
            this.sendClanMembersToUrl();
        }

    }

    /**
     * Creates a csv string from clan members
     *
     * @param clanMemberMaps Clan members info
     * @return csv with clan members info
     */
    private String toCSV(List<ClanMemberMap> clanMemberMaps) {
        String result = "";

        StringBuilder sb = new StringBuilder();

        for (ClanMemberMap clanMember : clanMemberMaps) {

            sb.append(clanMember.getRSN()).append(",");
            if (!this.config.getExportUserNamesOnly()) {
                sb.append(clanMember.getRank()).append(",");
                sb.append(clanMember.getJoinedDate()).append(",");
            }

            sb.append("\n");
        }

        result = sb.deleteCharAt(sb.length() - 1).toString();

        return result;
    }

    private String toJson(List<ClanMemberMap> clanMemberMaps) {
        return GSON.toJson(clanMemberMaps);
    }

    /**
     * Exports clanmembers to clip board
     */
    private void clanMembersToClipBoard(String clipboardString) {
        StringSelection stringSelection = new StringSelection(clipboardString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void sendClanMembersToUrl() {

        try {
            ClanMateExportWebRequestModel webRequestModel = new ClanMateExportWebRequestModel(this.clanName, this.clanMembers);

            final Request request = new Request.Builder()
                    .post(RequestBody.create(JSON, GSON.toJson(webRequestModel)))
                    .url(config.getDataUrl())
                    .build();

            RuneLiteAPI.CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    overlay.update(ClanMateExportOverlay.WhatToShow.WEB_REQUEST_FAILED);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                	if(response.isSuccessful()){
						overlay.update(ClanMateExportOverlay.WhatToShow.SUCCESS);
					}else{
						overlay.update(ClanMateExportOverlay.WhatToShow.WEB_REQUEST_FAILED);
					}

                }
            });
        } catch (Exception e) {
			overlay.update(ClanMateExportOverlay.WhatToShow.WEB_REQUEST_FAILED);

        }


    }
}
