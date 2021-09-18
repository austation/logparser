# Log Parser
This is a log parser for SS13 servers, written in Java by MCterra. It is intended to replace the PHP version used by /tg/station, which is very janky and requires linux to work.

Also the code is still kind of messy and needs some cleanup. Namely I need to move the config copier into another file visitor instead of it being a snowflake wrapper class.

This parser requires an API interface for the target server's `status` topic to function. The response to the request should look like this at minimum:
```
HTTP 200
Content-Type: application/json
```
```json
{
	"response": "Got status successfully",
	"data": {
		"round_id": 4925
	}
}
```
How this API is set up is your decision, but it should probably interface with some topic endpoint in `world_topic.dm` of your codebase.

**IF YOU USE LINUX AS YOUR HOSTING PLATFORM**, make sure to change the double backslashes in config.json to be forward slashes. No changes to the code itself are required, as I was careful to ensure it was platform-independant.