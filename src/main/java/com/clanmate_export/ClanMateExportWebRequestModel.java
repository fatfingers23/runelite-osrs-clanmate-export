package com.clanmate_export;

import java.util.List;


/**
 * This class is used only to create a json object with clan name and clan members
 */
public class ClanMateExportWebRequestModel {

    /**
     * The name of the clan
     */
    private String clanName;

    /**
     * List of clan members
     */
    private List<ClanMemberMap> clanMemberMaps;

    public ClanMateExportWebRequestModel(String clanName, List<ClanMemberMap> clanMateExportWebRequestModels){
        this.clanName = clanName;
        this.clanMemberMaps =clanMateExportWebRequestModels;
    }
}
