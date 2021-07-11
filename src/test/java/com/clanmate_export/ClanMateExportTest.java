package com.clanmate_export;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClanMateExportTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanMateExportPlugin.class);
		RuneLite.main(args);
	}
}