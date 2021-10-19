package au.id.mcterra.logparser;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Status {
	public final long roundId;

	public Status(String text) throws Exception {
		JSONParser parser = new JSONParser();
		Object result = parser.parse(text);
		JSONObject json = (JSONObject)result;
		roundId = (long)json.get("round_id");
	}
}
