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

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
public class ClanMateExportPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClanMateExportConfig config;
	@Inject
	private ClanMateExportChatMenuManager clanMateExportChatMenuManager;
	@Inject
	private OkHttpClient webClient;
	private static final Gson GSON = RuneLiteAPI.GSON;

	private static final int CLAN_SETTINGS_INFO_PAGE_WIDGET = 690;

	private static final int CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID = 693;
	private static final int CLAN_SETTINGS_MEMBERS_LIST_RSN_COLUMN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 10);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_FIRST_COLUMN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 11);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_SECOND_COLUMN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 13);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_FIRST_DROP_DOWN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 7);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_SECOND_DROP_DOWN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 8);


	/**
	 * The clan members, scraped from your clan setup widget
	 */
	private List<ClanMemberMap> clanMembers = null;

	@Provides
	ClanMateExportConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanMateExportConfig.class);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widget)
	{

		if (widget.getGroupId() == CLAN_SETTINGS_INFO_PAGE_WIDGET && config.getShowHelperText())
		{

			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.OPEN_MEMBERS_SCREEN);
		}

		if (widget.getGroupId() == CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID)
		{
			if (this.client.getWidget(693, 9) == null)
			{
				this.clanMembers = null;
			}
			else
			{
				clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SHOW_EXPORT_OPTIONS);
			}
		}
	}

	/**
	 * Subroutine - Update our memory of clan members and their ranks for
	 * clan setup
	 */
	public void scrapeMembers()
	{
		if (this.clanMembers == null)
		{
			this.clanMembers = new ArrayList<>();
		}
		this.clanMembers.clear();

		//Checks to set up scraping
		Widget clanMemberNamesWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_RSN_COLUMN);
		Widget rankWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_FIRST_COLUMN);
		Widget joinedWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_SECOND_COLUMN);

		//Checks to make sure drop downs are in correct location
		Widget[] leftColumnName = Objects.requireNonNull(this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_FIRST_DROP_DOWN)).getChildren();
		if (leftColumnName != null)
		{
			if (!leftColumnName[4].getText().equals("Rank"))
			{
				clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.CHECK_COLUMNS_RANKED);
				return;
			}
		}

		Widget[] rightColumnName = Objects.requireNonNull(this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_SECOND_DROP_DOWN)).getChildren();

		if (rightColumnName != null)
		{
			if (!rightColumnName[4].getText().equals("Joined"))
			{
				clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.CHECK_COLUMNS_JOINED);
				return;
			}

		}

		if (clanMemberNamesWidget == null || rankWidget == null || joinedWidget == null)
		{
			return;
		}
		Widget[] clanMemberNamesWidgetValues = clanMemberNamesWidget.getChildren();
		Widget[] rankWidgetValues = rankWidget.getChildren();
		Widget[] joinedWidgetValues = joinedWidget.getChildren();

		if (clanMemberNamesWidgetValues == null || rankWidgetValues == null || joinedWidgetValues == null)
		{
			return;
		}
		//Scrape all clan members

		int lastSuccessfulRsnIndex = 0;
		int otherColumnsPositions = 0;
		for (int i = 0; i < clanMemberNamesWidgetValues.length; i++)
		{
			int valueOfRsnToGet;
			if (i == 0)
			{
				valueOfRsnToGet = 1;
			}
			else
			{
				valueOfRsnToGet = lastSuccessfulRsnIndex + 3;
			}
			boolean inBounds = (valueOfRsnToGet >= 0) && (valueOfRsnToGet < clanMemberNamesWidgetValues.length);
			if (inBounds)
			{
				int clanMemberCount = Objects.requireNonNull(this.client.getClanSettings()).getMembers().size();
				int otherColumnsIndex = otherColumnsPositions + clanMemberCount;
				String rsn = Text.removeTags(clanMemberNamesWidgetValues[valueOfRsnToGet].getText());
				String rank = Text.removeTags(rankWidgetValues[otherColumnsIndex].getText());
				String joinedDate = Text.removeTags(joinedWidgetValues[otherColumnsIndex].getText());
				ClanMemberMap clanMember = new ClanMemberMap(rsn, rank, joinedDate);
				this.clanMembers.add(clanMember);
				lastSuccessfulRsnIndex = valueOfRsnToGet;
				otherColumnsPositions++;
			}
		}

	}

	public void ClanToClipBoard()
	{

		this.scrapeMembers();

		if (this.config.exportToClipBoard())
		{
			String clipBoardString = "";

			switch (this.config.getDataExportFormat())
			{
				case JSON:
					clipBoardString = toJson(this.clanMembers);
					break;
				case CSV:
					clipBoardString = toCSV(this.clanMembers);
					break;

			}

			this.clanMembersToClipBoard(clipBoardString);
			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SUCCESS);
		}
	}

	/**
	 * Creates a csv string from clan members
	 *
	 * @param clanMemberMaps Clan members info
	 * @return csv with clan members info
	 */
	private String toCSV(List<ClanMemberMap> clanMemberMaps)
	{
		String result = "";

		StringBuilder sb = new StringBuilder();

		for (ClanMemberMap clanMember : clanMemberMaps)
		{
			sb.append(clanMember.getRSN()).append(",");
			if (!this.config.getExportUserNamesOnly())
			{
				sb.append(clanMember.getRank()).append(",");
				sb.append(clanMember.getJoinedDate());
			}

			sb.append("\n");
		}

		result = sb.deleteCharAt(sb.length() - 1).toString();

		return result;
	}

	private String toJson(List<ClanMemberMap> clanMemberMaps)
	{
		return GSON.toJson(clanMemberMaps);
	}

	/**
	 * Exports clanmembers to clip board
	 */
	private void clanMembersToClipBoard(String clipboardString)
	{
		if(this.clanMembers.size() != 0)
		{
			StringSelection stringSelection = new StringSelection(clipboardString);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}
	}

	/**
	 * Exports clanmembers to remote url
	 *
	 * @return
	 */
	public void SendClanMembersToUrl()
	{
		this.scrapeMembers();
		if(this.clanMembers.size() != 0)
		{
			try
			{
				String clanName = Objects.requireNonNull(this.client.getClanSettings()).getName();
				ClanMateExportWebRequestModel webRequestModel = new ClanMateExportWebRequestModel(clanName, this.clanMembers);

				final Request request = new Request.Builder()
					.post(RequestBody.create(JSON, GSON.toJson(webRequestModel)))
					.url(config.getDataUrl())
					.build();

				webClient.newCall(request).enqueue(new Callback()
				{
					@Override
					public void onFailure(Call call, IOException e)
					{
						clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.WEB_REQUEST_FAILED);
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException
					{
						if (response.isSuccessful())
						{
							clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SUCCESS);
						}
						else
						{
							clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.WEB_REQUEST_FAILED);
						}

					}
				});
			}
			catch (Exception e)
			{
				clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.WEB_REQUEST_FAILED);

			}
		}
	}
}
