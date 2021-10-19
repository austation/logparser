package au.id.mcterra.logparser;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Status {
	public final int roundId;

	public Status(String text) throws Exception {
		JSONParser parser = new JSONParser();
		Object result = parser.parse(text);
		JSONObject json = (JSONObject)result;
		roundId = (int)json.get("round_id");
	}
}
