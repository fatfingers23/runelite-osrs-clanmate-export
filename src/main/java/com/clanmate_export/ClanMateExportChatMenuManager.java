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

import com.google.common.util.concurrent.Runnables;
import net.runelite.client.game.chatbox.ChatboxPanelManager;


import javax.inject.Inject;
import net.runelite.client.game.chatbox.ChatboxTextMenuInput;

public class ClanMateExportChatMenuManager
{

	/**
	 * A reference to the plugin object
	 */
	private final ClanMateExportPlugin plugin;

	private final ChatboxPanelManager chatboxPanelManager;


	@Inject
	private ClanMateExportConfig config;

	@Inject
	public ClanMateExportChatMenuManager(ClanMateExportPlugin plugin, ChatboxPanelManager chatboxPanelManager, ClanMateExportConfig config)
	{


		this.chatboxPanelManager = chatboxPanelManager;
		this.plugin = plugin;
		this.config = config;
	}

	public enum WhatToShow
	{
		OPEN_MEMBERS_SCREEN,
		CHECK_COLUMNS_JOINED,
		CHECK_COLUMNS_RANKED,
		SUCCESS,
		SHOW_EXPORT_OPTIONS,
		WEB_REQUEST_FAILED
	}

	/**
	 * Creates chat menu options to help the user
	 */
	public void update(WhatToShow whatToShow)
	{

		switch (whatToShow)
		{
			case OPEN_MEMBERS_SCREEN:
				this.chatboxPanelManager.openTextMenuInput(
						"To export Clanmembers click 'Members' on left side. <br> (This can be disabled in the plugin settings).")
					.option("Okay", Runnables.doNothing())
					.build();
				return;
			case CHECK_COLUMNS_JOINED:

				ChatboxTextMenuInput checkColumnsJoined = this.chatboxPanelManager.openTextMenuInput("Make Sure 'Joined' is selected as the last column.");
				addChoices(checkColumnsJoined);
				checkColumnsJoined.build();
				return;
			case CHECK_COLUMNS_RANKED:
				ChatboxTextMenuInput checkColumnsRanked = this.chatboxPanelManager.openTextMenuInput("Make Sure 'Ranked' is selected as the <br> middle column.");
				addChoices(checkColumnsRanked);
				checkColumnsRanked.build();
				return;
			case SUCCESS:
				this.chatboxPanelManager.openTextMenuInput("Clanmates have been exported. Can close the screen")
					.option("Okay", Runnables.doNothing()).build();
				return;

			case WEB_REQUEST_FAILED:
				this.chatboxPanelManager.openTextMenuInput("Web request failed.")
					.option("Okay", Runnables.doNothing()).build();
				return;
			case SHOW_EXPORT_OPTIONS:
				ChatboxTextMenuInput exportOptions = this.chatboxPanelManager.openTextMenuInput("Select an export option.");
				addChoices(exportOptions);
				exportOptions.build();
		}
	}

	/**
	 * Adds the export options to the chat menu
	 * @param chatboxTextMenuInput
	 */
	private void addChoices(ChatboxTextMenuInput chatboxTextMenuInput)
	{
		chatboxTextMenuInput.option("1. Export to your clipboard.", this.plugin::ClanToClipBoard);
		if (this.config.getSendWebRequest())
		{
			chatboxTextMenuInput.option("2. Export to the recorded URL.", this.plugin::SendClanMembersToUrl);
			chatboxTextMenuInput.option("3. Cancel.", Runnables.doNothing());
		}
		else
		{
			chatboxTextMenuInput.option("2. Cancel.", Runnables.doNothing());
		}
	}
}
