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

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ClanMateExportOverlay extends Overlay {

    /**
     * A reference to the plugin object
     */
    private final ClanMateExportPlugin plugin;

    /**
     * The UI Component
     */
    private final PanelComponent panelComponent;

    @Inject
    private ClanMateExportConfig config;

    @Inject
    public ClanMateExportOverlay(ClanMateExportPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        this.plugin = plugin;
        this.panelComponent = new PanelComponent();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        //Only show this when the clan setup widget is up
        return panelComponent.render(graphics);
    }

    public enum WhatToShow{
        OPEN_CLAN_MENU,
        OPEN_MEMBERS_SCREEN,
        CHECK_COLUMNS_JOINED,
        CHECK_COLUMNS_RANKED,
        SUCCESS,
        REMOVE,
        WEB_REQUEST_FAILED
    }

    /**
     * Subroutine - Update the user interface. This is quite a beefy process
     * so this method is only called when there's the possibility of a
     * potential UI change.
     */
    public void update(WhatToShow whatToShow) {
        panelComponent.setPreferredSize(new Dimension(250, 0));
        panelComponent.getChildren().clear();

        switch (whatToShow){
            case OPEN_CLAN_MENU:
                panelComponent.getChildren().add(TitleComponent.builder().text("Open 'Clan Settings'").build());
                return;
            case OPEN_MEMBERS_SCREEN:
                panelComponent.getChildren().add(TitleComponent.builder().text("Click 'Members' on left side").build());
                return;
            case CHECK_COLUMNS_JOINED:
                panelComponent.setPreferredSize(new Dimension(500, 50));
                panelComponent.getChildren().add(TitleComponent.builder().text("" +
                        "Make Sure 'Joined' is selected as the last column." +
                        "Once done click back " +
                        "").color(Color.RED).build());
                return;
            case CHECK_COLUMNS_RANKED:
                panelComponent.setPreferredSize(new Dimension(500, 50));
                panelComponent.getChildren().add(TitleComponent.builder().text("" +
                        "Make Sure 'Ranked' is selected as the middle column." +
                        "Once done click back" +
                        "").color(Color.RED).build());
                return;
            case SUCCESS:
                panelComponent.setPreferredSize(new Dimension(300, 10));
                panelComponent.getChildren().add(TitleComponent.builder().text("Clanmates have been exported. Can close the screen").build());
                return;
            case REMOVE:
                panelComponent.getChildren().clear();
                return;
            case WEB_REQUEST_FAILED:
                panelComponent.getChildren().add(TitleComponent.builder().text("Web request failed. to " + this.config.getDataUrl()).build());
                return;
        }

        //We're good to go! Build the renderable interface for clan actions.
//        panelComponent.getChildren().add(TitleComponent.builder().text("Clan Roster Actions").build());
//        qualifyActions();
    }
}
