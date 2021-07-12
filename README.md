# Clanmate Export
This is a plugin that is used to export Clan members and their details.

## How to use
1. Once logged into the game open the Clan interface via Clan Chat channel 'Settings' button.
2. Click 'Members' on the left side. Make sure the middle column is set to 'Rank' and last Column is 'Joined'.
3. Export should now be in your clipboard ready to paste.

### Features
* Export Clan members username,rank, and date joined in CSV or JSON.
* Export to clipboard
* Can create a post to an url of your choosing.
* More to come

 



Web request body example for export via web request
```json
{
  "clanName": "Name of your clan",
  "clanMemberMaps": [
    {
      "rsn": "ClanMember 1",
      "rank": "Sapphire",
      "joinedDate": "19-Jun-2021"
    },
    {
      "rsn": "ClanMember 2",
      "rank": "Sapphire",
      "joinedDate": "4-Jul-2021"
    }
  ]
}
```


## Special Thanks
This plugin is loosely based off of [Clan Roster Helper](https://github.com/simbleau/third-party-roster). 
Some code may be present in this repo since I used their plugin as an example.